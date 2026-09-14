package com.severus.finance.loan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordPaymentRequest(
    @NotNull(message = "Payment date is required")
    LocalDate paidDate,

    @NotNull(message = "Actual paid amount is required")
    @Min(value = 0, message = "Paid amount must be positive")
    BigDecimal actualPaidAmount,

    String notes
) {}
