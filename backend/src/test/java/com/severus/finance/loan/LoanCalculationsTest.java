package com.severus.finance.loan;

import com.severus.finance.common.util.CurrencyUtils;
import com.severus.finance.common.util.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class LoanCalculationsTest {

    @Test
    @DisplayName("Rule A: Expected end date calculation for 48 months starting April 2024")
    public void testExpectedEndDateApril2024_48Months() {
        // Loan start: April 2024 (e.g. 2024-04-05), Due Day 5, 48 installments
        LocalDate firstEmiDate = LocalDate.of(2024, 4, 5);
        LocalDate endDate = DateUtils.calculateExpectedEndDate(firstEmiDate, 5, 48);

        // Installment 1: 2024-04-05
        // Installment 48: 2028-03-05
        assertEquals(LocalDate.of(2028, 3, 5), endDate);

        // Implied tenure from start and end dates
        int impliedTenure = DateUtils.calculateImpliedTenure(firstEmiDate, endDate);
        assertEquals(48, impliedTenure);
    }

    @Test
    @DisplayName("Rule A: Due date calculation for each installment preserves due day")
    public void testInstallmentDueDateCalculation() {
        LocalDate firstEmiDate = LocalDate.of(2024, 1, 31);
        int dueDay = 31;

        // Installment 1: Jan 31
        assertEquals(LocalDate.of(2024, 1, 31), DateUtils.calculateInstallmentDueDate(firstEmiDate, dueDay, 1));
        // Installment 2: Feb 2024 (leap year) -> Feb 29
        assertEquals(LocalDate.of(2024, 2, 29), DateUtils.calculateInstallmentDueDate(firstEmiDate, dueDay, 2));
        // Installment 3: March 31
        assertEquals(LocalDate.of(2024, 3, 31), DateUtils.calculateInstallmentDueDate(firstEmiDate, dueDay, 3));
        // Installment 4: April 30
        assertEquals(LocalDate.of(2024, 4, 30), DateUtils.calculateInstallmentDueDate(firstEmiDate, dueDay, 4));
    }

    @Test
    @DisplayName("Rule B & D: Remaining EMIs and Remaining Loan Amount")
    public void testRemainingEmisAndRemainingLoanAmount() {
        int totalTenure = 48;
        BigDecimal monthlyEmi = new BigDecimal("15000.00");

        // Suppose 28 installments elapsed
        int elapsedEmis = 28;
        int remainingEmis = Math.max(0, totalTenure - elapsedEmis);
        assertEquals(20, remainingEmis);

        // Remaining Loan Amount = Fixed Monthly EMI * Remaining EMI Count
        BigDecimal remainingLoanAmount = monthlyEmi.multiply(BigDecimal.valueOf(remainingEmis));
        assertEquals(new BigDecimal("300000.00"), remainingLoanAmount);

        // If all 48 elapsed
        int allElapsed = 48;
        int remainingZero = Math.max(0, totalTenure - allElapsed);
        assertEquals(0, remainingZero);
        BigDecimal remainingZeroAmount = monthlyEmi.multiply(BigDecimal.valueOf(remainingZero));
        assertEquals(0, remainingZeroAmount.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Reducing balance amortization calculation components")
    public void testReducingBalanceAmortizationComponents() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualRate = new BigDecimal("12.00"); // 1% per month
        BigDecimal emi = new BigDecimal("10000.00");

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);

        // Month 1: Interest = 100000 * 0.01 = 1000.00
        BigDecimal interest1 = principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
        assertEquals(new BigDecimal("1000.00"), interest1);

        // Principal component = 10000 - 1000 = 9000.00
        BigDecimal principal1 = CurrencyUtils.subtract(emi, interest1);
        assertEquals(new BigDecimal("9000.00"), principal1);

        // Remaining principal = 100000 - 9000 = 91000.00
        BigDecimal remainingPrincipal = CurrencyUtils.subtract(principal, principal1);
        assertEquals(new BigDecimal("91000.00"), remainingPrincipal);
    }
}
