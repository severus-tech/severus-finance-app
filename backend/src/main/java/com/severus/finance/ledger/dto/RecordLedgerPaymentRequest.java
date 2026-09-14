package com.severus.finance.ledger.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordLedgerPaymentRequest(
    @NotNull(message = "Payment amount is required")
    @Min(value = 1, message = "Payment amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    Boolean allowOverpayment,
    String notes
) {}
