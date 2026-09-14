package com.severus.finance.loan.dto;

import java.math.BigDecimal;

public record LoanSummaryResponse(
    BigDecimal totalOriginalPrincipal,
    BigDecimal totalOutstandingPrincipal,
    BigDecimal totalRemainingRepayment,
    BigDecimal totalEstimatedRemainingInterest,
    BigDecimal totalMonthlyEmi,
    long activeLoanCount,
    int totalPaidEmiCount,
    int totalRemainingEmiCount
) {}
