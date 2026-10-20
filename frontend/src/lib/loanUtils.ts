import { Loan } from '@/types';

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
  totalEmis: number;
  annualInterestRate: number;
  nextEmiDate: string;
  lastEmiDate: string;
  progressPercent: number;
  repaidPrincipal: number;
  insight: string;
}

export function computeLoanMetrics(loan: Loan): CalculatedLoanMetrics {
  const originalPrincipal = loan.originalPrincipal || 0;
  const principalRemaining = loan.currentOutstandingPrincipal || 0;
  const emiAmount = loan.emiAmount || 0;
  const monthlyEmiFreed = emiAmount;

  // Compute remaining & paid EMI counts
  let remainingEmis = loan.remainingEmiCount;
  let paidEmis = loan.paidEmiCount;
  if (loan.installments && loan.installments.length > 0) {
    const unpaidList = loan.installments.filter((i) => i.paymentState === 'UNPAID');
    const paidList = loan.installments.filter((i) => i.paymentState === 'PAID');
    remainingEmis = unpaidList.length;
    paidEmis = paidList.length;
  }
  remainingEmis = remainingEmis ?? Math.max(0, (loan.totalEmiCount || 0) - (loan.paidEmiCount || 0));
  paidEmis = paidEmis ?? 0;
  const totalEmis = loan.totalEmiCount || (paidEmis + remainingEmis);

  // Total Loan Balance: sum of remaining EMIs (EMI × remaining months or scheduled amounts)
  let totalLoanBalance = loan.remainingRepayment;
  if (!totalLoanBalance || totalLoanBalance <= 0) {
    totalLoanBalance = emiAmount * remainingEmis;
  }

  // Repaid principal and progress %
  const repaidPrincipal = Math.max(0, originalPrincipal - principalRemaining);
  const progressPercent =
    originalPrincipal > 0
      ? Math.min(100, Math.max(0, Math.round((repaidPrincipal / originalPrincipal) * 100)))
      : 0;

  // Dates
  let nextEmiDate = '';
  let lastEmiDate = '';
  const todayStr = new Date().toISOString().split('T')[0];

  if (loan.installments && loan.installments.length > 0) {
    // Next EMI from current day: first UNPAID installment due today or in future, or nearest unpaid
    const futureUnpaid = loan.installments.find(
      (i) => i.paymentState === 'UNPAID' && i.dueDate >= todayStr
    );
    const anyUnpaid = loan.installments.find((i) => i.paymentState === 'UNPAID');
    const nextInst = futureUnpaid || anyUnpaid;
    if (nextInst) {
      nextEmiDate = nextInst.dueDate;
    }
    const lastInst = loan.installments[loan.installments.length - 1];
    if (lastInst) {
      lastEmiDate = lastInst.dueDate;
    }
  }

  // Fallback dates if installments array is absent or empty
  const isLoanClosed = loan.status === 'CLOSED' || (remainingEmis !== undefined && remainingEmis <= 0);

  if (!nextEmiDate && !isLoanClosed) {
    const now = new Date();
    const currentDay = now.getDate();
    let targetYear = now.getFullYear();
    let targetMonth = now.getMonth();

    const dueDaySetting = loan.emiDueDay || (loan.startDate ? new Date(loan.startDate).getDate() : 5);
    if (currentDay > dueDaySetting) {
      targetMonth += 1;
    }
    const daysInTargetMonth = new Date(targetYear, targetMonth + 1, 0).getDate();
    const resolvedDay = Math.min(dueDaySetting, daysInTargetMonth);
    const nextDateObj = new Date(targetYear, targetMonth, resolvedDay);
    nextEmiDate = nextDateObj.toISOString().split('T')[0];
  }

  if (!lastEmiDate && (loan.startDate || loan.firstEmiDate) && totalEmis > 0) {
    try {
      const d = new Date(loan.startDate || loan.firstEmiDate);
      d.setMonth(d.getMonth() + totalEmis - 1);
      lastEmiDate = d.toISOString().split('T')[0];
    } catch {
      lastEmiDate = '';
    }
  }

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
    totalEmis,
    annualInterestRate: loan.annualInterestRate || 0,
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
