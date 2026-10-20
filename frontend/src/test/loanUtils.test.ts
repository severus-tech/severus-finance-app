import { describe, it, expect } from 'vitest';
import {
  computeLoanMetrics,
  calculateExpectedEndDate,
  calculateTenureMonths,
} from '../lib/loanUtils';
import { Loan } from '../types';

describe('Loan Utils - Business Rules', () => {
  it('Rule A: calculateExpectedEndDate correctly computes end date for 48 months tenure', () => {
    // April 2024 with 48 months tenure -> March 2028
    const endDate = calculateExpectedEndDate('2024-04-05', 48, 5);
    expect(endDate).toBe('2028-03-05');
  });

  it('calculateTenureMonths correctly computes tenure between start date and end date', () => {
    const tenure = calculateTenureMonths('2024-04-05', '2028-03-05');
    expect(tenure).toBe(48);
  });

  it('Rule A & Rule B & Rule D: computeLoanMetrics separates elapsed vs actually paid and computes fixed commitment', () => {
    const sampleLoan: Loan = {
      id: 'test-1',
      name: 'Personal Loan',
      lenderOrBank: 'HDFC Bank',
      loanType: 'PERSONAL_LOAN',
      originalPrincipal: 500000,
      currentOutstandingPrincipal: 250000,
      remainingRepayment: 0,
      estimatedRemainingInterest: 0,
      annualInterestRate: 10,
      startDate: '2024-01-05',
      firstEmiDate: '2024-01-05',
      endDate: '2027-12-05',
      emiDueDay: 5,
      emiAmount: 15000,
      originalTenureMonths: 48,
      totalEmiCount: 48,
      paidEmiCount: 5,
      remainingEmiCount: 43,
      calculationMode: 'REDUCING_BALANCE',
      outstandingPrincipalManualOverride: false,
      status: 'ACTIVE',
      version: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      installments: [
        // 5 past installments marked PAID
        { id: '1', loanId: 'test-1', installmentNumber: 1, dueDate: '2024-01-05', scheduledAmount: 15000, principalComponent: 10000, interestComponent: 5000, paymentState: 'PAID', paidDate: '2024-01-05', version: 1, createdAt: '', updatedAt: '' },
        { id: '2', loanId: 'test-1', installmentNumber: 2, dueDate: '2024-02-05', scheduledAmount: 15000, principalComponent: 10000, interestComponent: 5000, paymentState: 'PAID', paidDate: '2024-02-05', version: 1, createdAt: '', updatedAt: '' },
        { id: '3', loanId: 'test-1', installmentNumber: 3, dueDate: '2024-03-05', scheduledAmount: 15000, principalComponent: 10000, interestComponent: 5000, paymentState: 'PAID', paidDate: '2024-03-05', version: 1, createdAt: '', updatedAt: '' },
        { id: '4', loanId: 'test-1', installmentNumber: 4, dueDate: '2024-04-05', scheduledAmount: 15000, principalComponent: 10000, interestComponent: 5000, paymentState: 'PAID', paidDate: '2024-04-05', version: 1, createdAt: '', updatedAt: '' },
        { id: '5', loanId: 'test-1', installmentNumber: 5, dueDate: '2024-05-05', scheduledAmount: 15000, principalComponent: 10000, interestComponent: 5000, paymentState: 'PAID', paidDate: '2024-05-05', version: 1, createdAt: '', updatedAt: '' },
        // 1 past installment UNPAID (overdue)
        { id: '6', loanId: 'test-1', installmentNumber: 6, dueDate: '2024-06-05', scheduledAmount: 15000, principalComponent: 10000, interestComponent: 5000, paymentState: 'UNPAID', version: 1, createdAt: '', updatedAt: '' },
        // 1 future installment UNPAID
        { id: '7', loanId: 'test-1', installmentNumber: 7, dueDate: '2099-01-05', scheduledAmount: 15000, principalComponent: 10000, interestComponent: 5000, paymentState: 'UNPAID', version: 1, createdAt: '', updatedAt: '' },
      ],
    };

    const metrics = computeLoanMetrics(sampleLoan);

    // Rule A: Tenure fixed
    expect(metrics.totalEmis).toBe(48);
    // Rule C: Elapsed includes all due dates <= today (6 installments), but paid is strictly 5
    expect(metrics.elapsedEmis).toBe(6);
    expect(metrics.paidEmis).toBe(5);
    expect(metrics.overdueEmis).toBe(1);

    // Rule B: Remaining = Math.max(0, 48 - 6) = 42
    expect(metrics.remainingEmis).toBe(42);

    // Rule D: Remaining Loan Amount = 42 * 15,000 = 630,000
    expect(metrics.totalLoanBalance).toBe(630000);

    // Rule 5: Next EMI is the upcoming future unpaid installment
    expect(metrics.nextEmiDate).toBe('2099-01-05');
  });

  it('Rule B: When last EMI date has passed, remaining EMIs is zero and remaining loan amount is zero', () => {
    const expiredLoan: Loan = {
      id: 'test-expired',
      name: 'Old Loan',
      lenderOrBank: 'SBI',
      loanType: 'PERSONAL_LOAN',
      originalPrincipal: 100000,
      currentOutstandingPrincipal: 0,
      remainingRepayment: 0,
      estimatedRemainingInterest: 0,
      annualInterestRate: 8,
      startDate: '2020-01-05',
      firstEmiDate: '2020-01-05',
      endDate: '2020-03-05',
      emiDueDay: 5,
      emiAmount: 35000,
      originalTenureMonths: 3,
      totalEmiCount: 3,
      paidEmiCount: 3,
      remainingEmiCount: 0,
      calculationMode: 'REDUCING_BALANCE',
      outstandingPrincipalManualOverride: false,
      status: 'CLOSED',
      version: 1,
      createdAt: '',
      updatedAt: '',
      installments: [
        { id: '1', loanId: 'test-expired', installmentNumber: 1, dueDate: '2020-01-05', scheduledAmount: 35000, principalComponent: 33000, interestComponent: 2000, paymentState: 'PAID', version: 1, createdAt: '', updatedAt: '' },
        { id: '2', loanId: 'test-expired', installmentNumber: 2, dueDate: '2020-02-05', scheduledAmount: 35000, principalComponent: 33500, interestComponent: 1500, paymentState: 'PAID', version: 1, createdAt: '', updatedAt: '' },
        { id: '3', loanId: 'test-expired', installmentNumber: 3, dueDate: '2020-03-05', scheduledAmount: 35000, principalComponent: 34000, interestComponent: 1000, paymentState: 'PAID', version: 1, createdAt: '', updatedAt: '' },
      ],
    };

    const metrics = computeLoanMetrics(expiredLoan);
    expect(metrics.totalEmis).toBe(3);
    expect(metrics.elapsedEmis).toBe(3);
    expect(metrics.remainingEmis).toBe(0);
    expect(metrics.totalLoanBalance).toBe(0);
  });
});

