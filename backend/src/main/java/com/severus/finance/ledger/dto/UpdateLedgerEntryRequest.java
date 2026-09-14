package com.severus.finance.ledger.dto;

import com.severus.finance.ledger.enums.LedgerDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateLedgerEntryRequest(
    @NotBlank(message = "Person name is required")
    String personName,

    @NotNull(message = "Direction is required")
    LedgerDirection direction,

    @NotNull(message = "Entry date is required")
    LocalDate entryDate,

    LocalDate dueDate,
    String description,
    String notes
) {}
