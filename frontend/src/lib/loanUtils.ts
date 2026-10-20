import { Loan } from '@/types';
import { api } from '@/lib/api';

export type LoanSortOption =
  | 'SMALLEST_BALANCE'
  | 'HIGHEST_INTEREST'
  | 'NEAREST_CLOSURE'
  | 'HIGHEST_EMI_FREED';

export interface CalculatedLoanMetrics {
  originalPrincipal: number;
  principalRemaining: number;
  totalLoanBalance: number;
  emiAmount: number;
  monthlyEmiFreed: number;
  remainingEmis: number;
  paidEmis: number;
  elapsedEmis: number;
  overdueEmis: number;
  totalEmis: number;
  annualInterestRate: number;
  startDate: string;
  nextEmiDate: string;
  lastEmiDate: string;
  progressPercent: number;
  repaidPrincipal: number;
  insight: string;
}

/**
 * Calculates expected end date from first EMI date (or start date) and tenure in months.
 */
export function calculateExpectedEndDate(startDateStr: string, tenureMonths: number, dueDay?: number): string {
  if (!startDateStr || tenureMonths <= 0) return '';
  const [yearStr, monthStr, dayStr] = startDateStr.split('-');
  const startYear = parseInt(yearStr, 10);
  const startMonth = parseInt(monthStr, 10); // 1-based
  const day = dueDay ?? parseInt(dayStr, 10);

  // Month index for end date = startMonth - 1 + (tenureMonths - 1)
  const totalMonths = (startMonth - 1) + (tenureMonths - 1);
  const endYear = startYear + Math.floor(totalMonths / 12);
  const endMonth = (totalMonths % 12) + 1;

  const daysInMonth = new Date(endYear, endMonth, 0).getDate();
  const resolvedDay = Math.min(day, daysInMonth);

  const mm = String(endMonth).padStart(2, '0');
  const dd = String(resolvedDay).padStart(2, '0');
  return `${endYear}-${mm}-${dd}`;
}

/**
 * Calculates tenure in months from start date and end date.
 */
export function calculateTenureMonths(startDateStr: string, endDateStr: string): number {
  if (!startDateStr || !endDateStr) return 0;
  const [sYear, sMonth] = startDateStr.split('-').map(Number);
  const [eYear, eMonth] = endDateStr.split('-').map(Number);
  if (!sYear || !sMonth || !eYear || !eMonth) return 0;

  const diffMonths = (eYear - sYear) * 12 + (eMonth - sMonth) + 1;
  return Math.max(1, diffMonths);
}

export function computeLoanMetrics(loan: Loan): CalculatedLoanMetrics {
  const originalPrincipal = loan.originalPrincipal || 0;
  const principalRemaining = loan.remainingPrincipal ?? loan.currentOutstandingPrincipal ?? 0;
  const emiAmount = loan.emiAmount || 0;
  const monthlyEmiFreed = emiAmount;

  // Rule A: Original tenure must remain fixed and NEVER be derived from paid + unpaid
  const totalEmis = loan.totalEmiCount || loan.originalTenureMonths || (loan.installments ? loan.installments.length : 1);

  const todayStr = new Date().toISOString().split('T')[0];

  let elapsedEmis = 0;
  let paidEmis = 0;
  let overdueEmis = 0;
  let remainingEmis = 0;
  let nextEmiDate = '';
  let lastEmiDate = loan.endDate || '';

  if (loan.installments && loan.installments.length > 0) {
    // Sort installments by number
    const sorted = [...loan.installments].sort((a, b) => a.installmentNumber - b.installmentNumber);

    for (const inst of sorted) {
      const isPastOrToday = inst.dueDate <= todayStr;
      const isPast = inst.dueDate < todayStr;
      const isPaid = inst.paymentState === 'PAID';

      if (isPastOrToday) {
        elapsedEmis++;
      }
      if (isPaid) {
        paidEmis++;
      }
      if (isPast && !isPaid) {
        overdueEmis++;
      }
    }

    // Elapsed capped at totalEmis
    elapsedEmis = Math.min(totalEmis, elapsedEmis);

    // Rule B: Remaining = Math.max(0, totalEmis - elapsedEmis). Zero if schedule end has passed.
    remainingEmis = Math.max(0, totalEmis - elapsedEmis);

    // Rule 5: Next EMI Date should be the next emi from the current day
    const futureUnpaid = sorted.find((i) => i.paymentState === 'UNPAID' && i.dueDate >= todayStr);
    if (futureUnpaid) {
      nextEmiDate = futureUnpaid.dueDate;
    }

    if (!lastEmiDate && sorted.length > 0) {
      lastEmiDate = sorted[sorted.length - 1].dueDate;
    }
  } else {
    // Fallback if installments array is not loaded
    paidEmis = loan.paidEmiCount || 0;
    elapsedEmis = loan.elapsedEmiCount ?? paidEmis;
    remainingEmis = loan.remainingEmiCount ?? Math.max(0, totalEmis - elapsedEmis);
    overdueEmis = loan.overdueEmiCount ?? 0;
    nextEmiDate = loan.nextEmiDate || '';
  }

  // Fallback for lastEmiDate
  if (!lastEmiDate && (loan.startDate || loan.firstEmiDate) && totalEmis > 0) {
    lastEmiDate = calculateExpectedEndDate(loan.firstEmiDate || loan.startDate, totalEmis, loan.emiDueDay);
  }

  // Fallback for nextEmiDate if loan still has remaining EMIs and nextEmiDate not set
  if (!nextEmiDate && remainingEmis > 0 && loan.status !== 'CLOSED') {
    const now = new Date();
    const currentDay = now.getDate();
    let targetYear = now.getFullYear();
    let targetMonth = now.getMonth();

    const dueDaySetting = loan.emiDueDay || (loan.startDate ? parseInt(loan.startDate.split('-')[2], 10) : 5);
    if (currentDay > dueDaySetting) {
      targetMonth += 1;
    }
    const daysInTargetMonth = new Date(targetYear, targetMonth + 1, 0).getDate();
    const resolvedDay = Math.min(dueDaySetting, daysInTargetMonth);
    const nextDateObj = new Date(targetYear, targetMonth, resolvedDay);
    nextEmiDate = nextDateObj.toISOString().split('T')[0];
  }

  // Rule D: Remaining Loan Amount = Fixed Monthly EMI × Remaining EMI Count (independent of principal)
  const totalLoanBalance = remainingEmis > 0 ? emiAmount * remainingEmis : 0;

  // Repaid principal and progress %
  const repaidPrincipal = Math.max(0, originalPrincipal - principalRemaining);
  const progressPercent =
    totalEmis > 0
      ? Math.min(100, Math.max(0, Math.round((paidEmis / totalEmis) * 100)))
      : 0;

  // Insights
  let insight = `Close this loan to free ₹${Math.round(monthlyEmiFreed).toLocaleString('en-IN')}/month EMI`;
  if (remainingEmis > 0 && remainingEmis <= 6) {
    insight = `Only ${remainingEmis} EMI${remainingEmis === 1 ? '' : 's'} left to close!`;
  } else if (loan.annualInterestRate >= 12) {
    insight = `High interest (${loan.annualInterestRate}% p.a.) — save money by closing early`;
  } else if (totalLoanBalance < 50000 && remainingEmis > 0) {
    insight = `Small remaining balance of ₹${Math.round(totalLoanBalance).toLocaleString('en-IN')}`;
  }

  return {
    originalPrincipal,
    principalRemaining,
    totalLoanBalance,
    emiAmount,
    monthlyEmiFreed,
    remainingEmis,
    paidEmis,
    elapsedEmis,
    overdueEmis,
    totalEmis,
    annualInterestRate: loan.annualInterestRate || 0,
    startDate: loan.startDate,
    nextEmiDate,
    lastEmiDate,
    progressPercent,
    repaidPrincipal,
    insight,
  };
}

export function sortLoansByFocus(loans: Loan[], sortBy: LoanSortOption): Loan[] {
  const list = [...loans];

  switch (sortBy) {
    case 'SMALLEST_BALANCE':
      return list.sort((a, b) => {
        const balA = computeLoanMetrics(a).totalLoanBalance;
        const balB = computeLoanMetrics(b).totalLoanBalance;
        return balA - balB;
      });

    case 'HIGHEST_INTEREST':
      return list.sort((a, b) => {
        const intA = a.annualInterestRate || 0;
        const intB = b.annualInterestRate || 0;
        return intB - intA;
      });

    case 'NEAREST_CLOSURE':
      return list.sort((a, b) => {
        const dateA = computeLoanMetrics(a).lastEmiDate;
        const dateB = computeLoanMetrics(b).lastEmiDate;
        if (!dateA) return 1;
        if (!dateB) return -1;
        return new Date(dateA).getTime() - new Date(dateB).getTime();
      });

    case 'HIGHEST_EMI_FREED':
      return list.sort((a, b) => {
        return (b.emiAmount || 0) - (a.emiAmount || 0);
      });

    default:
      return list;
  }
}

/**
 * Downloads the authoritative Loan Portfolio PDF report from the backend.
 */
export async function downloadLoanPortfolioPdf(): Promise<void> {
  const response = await api.get('/api/v1/loans/export/pdf', {
    responseType: 'blob',
  });

  let filename = `Finance_Loan_Portfolio_${new Date().toISOString().split('T')[0]}.pdf`;
  const disposition = response.headers['content-disposition'];
  if (disposition && disposition.includes('filename=')) {
    const match = disposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) {
      filename = match[1];
    }
  }

  const blob = new Blob([response.data], { type: 'application/pdf' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.parentNode?.removeChild(link);
  window.URL.revokeObjectURL(url);
}
