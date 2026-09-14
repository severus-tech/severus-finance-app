package com.severus.finance.loan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrepaymentRequest(
    @NotNull(message = "Prepayment amount is required")
    @Min(value = 1, message = "Prepayment amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    Boolean keepSameEmiReduceTenure,

    String notes
) {}
