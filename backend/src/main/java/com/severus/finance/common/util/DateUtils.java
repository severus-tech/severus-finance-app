package com.severus.finance.common.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateUtils {

    public static final ZoneId ZONE_KOLKATA = ZoneId.of("Asia/Kolkata");

    public static LocalDate todayInKolkata() {
        return ZonedDateTime.now(ZONE_KOLKATA).toLocalDate();
    }

    public static LocalDate resolveDueDate(int year, int month, int targetDay) {
        YearMonth ym = YearMonth.of(year, month);
        int lastDay = ym.lengthOfMonth();
        int validDay = Math.min(targetDay, lastDay);
        return LocalDate.of(year, month, validDay);
    }

    public static LocalDate calculateInstallmentDueDate(LocalDate firstEmiDate, int emiDueDay, int installmentNumber) {
        if (firstEmiDate == null || installmentNumber <= 1) {
            return firstEmiDate;
        }
        YearMonth ym = YearMonth.from(firstEmiDate).plusMonths(installmentNumber - 1);
        return resolveDueDate(ym.getYear(), ym.getMonthValue(), emiDueDay);
    }

    public static LocalDate calculateExpectedEndDate(LocalDate firstEmiDate, int emiDueDay, int totalEmiCount) {
        if (firstEmiDate == null || totalEmiCount <= 0) {
            return firstEmiDate;
        }
        return calculateInstallmentDueDate(firstEmiDate, emiDueDay, totalEmiCount);
    }

    public static int calculateImpliedTenure(LocalDate firstEmiDate, LocalDate targetEndDate) {
        if (targetEndDate == null || firstEmiDate == null || targetEndDate.isBefore(firstEmiDate)) {
            return 1;
        }
        long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(
                YearMonth.from(firstEmiDate),
                YearMonth.from(targetEndDate)
        );
        return Math.max(1, (int) monthsBetween + 1);
    }

    public static SalaryCycle calculateSalaryCycle(LocalDate asOfDate, int salaryCreditDay) {
        LocalDate today = asOfDate != null ? asOfDate : todayInKolkata();

        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        int dayOfMonth = today.getDayOfMonth();

        LocalDate cycleStart;
        LocalDate cycleEnd;

        if (dayOfMonth >= salaryCreditDay) {
            cycleStart = resolveDueDate(currentYear, currentMonth, salaryCreditDay);
            YearMonth nextYm = YearMonth.of(currentYear, currentMonth).plusMonths(1);
            LocalDate nextCreditDate = resolveDueDate(nextYm.getYear(), nextYm.getMonthValue(), salaryCreditDay);
            cycleEnd = nextCreditDate.minusDays(1);
        } else {
            YearMonth prevYm = YearMonth.of(currentYear, currentMonth).minusMonths(1);
            cycleStart = resolveDueDate(prevYm.getYear(), prevYm.getMonthValue(), salaryCreditDay);
            LocalDate currentCreditDate = resolveDueDate(currentYear, currentMonth, salaryCreditDay);
            cycleEnd = currentCreditDate.minusDays(1);
        }

        return new SalaryCycle(cycleStart, cycleEnd);
    }

    public record SalaryCycle(LocalDate startDate, LocalDate endDate) {}
}
