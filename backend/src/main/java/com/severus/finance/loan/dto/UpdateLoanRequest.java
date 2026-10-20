package com.severus.finance.loan.dto;

import com.severus.finance.loan.enums.LoanStatus;
import com.severus.finance.loan.enums.LoanType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateLoanRequest(
    @NotBlank(message = "Loan name is required")
    String name,

    @NotBlank(message = "Lender or bank name is required")
    String lenderOrBank,

    LoanType loanType,

    String maskedAccountNumber,

    BigDecimal currentOutstandingPrincipal,

    @NotNull(message = "Annual interest rate is required")
    @Min(value = 0, message = "Interest rate cannot be negative")
    BigDecimal annualInterestRate,

    @Min(value = 1, message = "EMI due day must be between 1 and 31")
    @Max(value = 31, message = "EMI due day must be between 1 and 31")
    int emiDueDay,

    @NotNull(message = "EMI amount is required")
    @Min(value = 1, message = "EMI amount must be positive")
    BigDecimal emiAmount,

    Boolean manualOverride,

    LoanStatus status,

    Boolean recalculateFutureSchedule,

    String notes
) {}
