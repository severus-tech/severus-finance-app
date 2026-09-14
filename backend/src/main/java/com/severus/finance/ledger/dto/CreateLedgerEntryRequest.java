package com.severus.finance.ledger.dto;

import com.severus.finance.ledger.enums.LedgerDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLedgerEntryRequest(
    @NotBlank(message = "Person name is required")
    String personName,

    @NotNull(message = "Direction is required")
    LedgerDirection direction,

    @NotNull(message = "Original amount is required")
    @Min(value = 1, message = "Original amount must be positive")
    BigDecimal originalAmount,

    @NotNull(message = "Entry date is required")
    LocalDate entryDate,

    LocalDate dueDate,
    String description,
    String notes
) {}
