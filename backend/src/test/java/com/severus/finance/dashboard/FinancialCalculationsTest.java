package com.severus.finance.dashboard;

import com.severus.finance.common.util.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class FinancialCalculationsTest {

    @Test
    @DisplayName("Salary cycle starting on credit day (e.g. Day 1)")
    public void testSalaryCycleCreditDay1() {
        LocalDate today = LocalDate.of(2026, 9, 3);
        DateUtils.SalaryCycle cycle = DateUtils.calculateSalaryCycle(today, 1);

        assertEquals(LocalDate.of(2026, 9, 1), cycle.startDate());
        assertEquals(LocalDate.of(2026, 9, 30), cycle.endDate());
    }

    @Test
    @DisplayName("Salary cycle before credit day (e.g. Day 5 when today is Sept 3)")
    public void testSalaryCycleBeforeCreditDay() {
        LocalDate today = LocalDate.of(2026, 9, 3);
        DateUtils.SalaryCycle cycle = DateUtils.calculateSalaryCycle(today, 5);

        // Cycle starts Aug 5 and ends Sept 4
        assertEquals(LocalDate.of(2026, 8, 5), cycle.startDate());
        assertEquals(LocalDate.of(2026, 9, 4), cycle.endDate());
    }

    @Test
    @DisplayName("Salary cycle on credit day (e.g. Day 5 when today is Sept 5)")
    public void testSalaryCycleOnCreditDay() {
        LocalDate today = LocalDate.of(2026, 9, 5);
        DateUtils.SalaryCycle cycle = DateUtils.calculateSalaryCycle(today, 5);

        // Cycle starts Sept 5 and ends Oct 4
        assertEquals(LocalDate.of(2026, 9, 5), cycle.startDate());
        assertEquals(LocalDate.of(2026, 10, 4), cycle.endDate());
    }

    @Test
    @DisplayName("Leap year February resolution for Day 31")
    public void testLeapYearFebruary() {
        LocalDate resolvedLeapFeb = DateUtils.resolveDueDate(2028, 2, 31);
        assertEquals(LocalDate.of(2028, 2, 29), resolvedLeapFeb);

        LocalDate resolvedNonLeapFeb = DateUtils.resolveDueDate(2026, 2, 31);
        assertEquals(LocalDate.of(2026, 2, 28), resolvedNonLeapFeb);
    }
}
