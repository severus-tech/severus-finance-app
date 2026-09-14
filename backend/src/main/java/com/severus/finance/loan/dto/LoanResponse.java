package com.severus.finance.loan.dto;

import com.severus.finance.loan.enums.CalculationMode;
import com.severus.finance.loan.enums.LoanStatus;
import com.severus.finance.loan.enums.LoanType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LoanResponse(
    UUID id,
    String name,
    String lenderOrBank,
    LoanType loanType,
    String maskedAccountNumber,
    BigDecimal originalPrincipal,
    BigDecimal currentOutstandingPrincipal,
    BigDecimal remainingRepayment,
    BigDecimal estimatedRemainingInterest,
    BigDecimal annualInterestRate,
    LocalDate startDate,
    LocalDate firstEmiDate,
    int emiDueDay,
    BigDecimal emiAmount,
    int originalTenureMonths,
    int totalEmiCount,
    int paidEmiCount,
    int remainingEmiCount,
    CalculationMode calculationMode,
    boolean outstandingPrincipalManualOverride,
    LoanStatus status,
    String notes,
    Long version,
    List<LoanInstallmentResponse> installments,
    Instant createdAt,
    Instant updatedAt
) {}
