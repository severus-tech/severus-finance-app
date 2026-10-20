import { describe, it, expect } from 'vitest';
import { formatINR, formatDate } from '../lib/utils';
import { computeLoanMetrics } from '../lib/loanUtils';
import { Loan } from '../types';

describe('Frontend Utils', () => {
  it('formats INR numbers correctly', () => {
    expect(formatINR(500000)).toMatch(/5,00,000/);
    expect(formatINR(0)).toMatch(/0/);
    expect(formatINR(null)).toBe('₹0');
  });

  it('formats dates into readable format', () => {
    const formatted = formatDate('2026-09-05');
    expect(formatted).toContain('Sep');
    expect(formatted).toContain('2026');
  });

  it('computes next EMI date from current day', () => {
    const sampleLoan: Loan = {
      id: 'test-loan-1',
      name: 'Car Loan',
      lenderOrBank: 'HDFC',
      loanType: 'CAR_LOAN',
      originalPrincipal: 500000,
      currentOutstandingPrincipal: 400000,
      remainingRepayment: 450000,
      estimatedRemainingInterest: 50000,
      annualInterestRate: 9.5,
      startDate: '2025-01-05',
      firstEmiDate: '2025-01-05',
      emiDueDay: 5,
      emiAmount: 10500,
      originalTenureMonths: 60,
      totalEmiCount: 60,
      paidEmiCount: 10,
      remainingEmiCount: 50,
      calculationMode: 'REDUCING_BALANCE',
      outstandingPrincipalManualOverride: false,
      status: 'ACTIVE',
      version: 0,
      createdAt: '2025-01-05T00:00:00Z',
      updatedAt: '2025-01-05T00:00:00Z',
    };

    const metrics = computeLoanMetrics(sampleLoan);
    // Should NOT be the firstEmiDate from 2025!
    expect(metrics.nextEmiDate).not.toBe('2025-01-05');
    // Should be on or after today's year
    const nextYear = parseInt(metrics.nextEmiDate.split('-')[0], 10);
    expect(nextYear).toBeGreaterThanOrEqual(new Date().getFullYear());
  });
});
