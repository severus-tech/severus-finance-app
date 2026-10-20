package com.severus.finance.loan.dto;

import com.severus.finance.loan.enums.PaymentState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanInstallmentResponse(
    UUID id,
    UUID loanId,
    int installmentNumber,
    LocalDate dueDate,
    BigDecimal scheduledAmount,
    BigDecimal principalComponent,
    BigDecimal interestComponent,
    PaymentState paymentState,
    LocalDate paidDate,
    BigDecimal actualPaidAmount,
    String notes,
    Long version,
    boolean overdue,
    Instant createdAt,
    Instant updatedAt
) {}
