package com.severus.finance.ledger.dto;

import com.severus.finance.ledger.enums.LedgerDirection;
import com.severus.finance.ledger.enums.LedgerStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LedgerEntryResponse(
    UUID id,
    String personName,
    LedgerDirection direction,
    BigDecimal originalAmount,
    BigDecimal remainingBalance,
    LocalDate entryDate,
    LocalDate dueDate,
    String description,
    LedgerStatus status,
    String notes,
    Long version,
    List<LedgerPaymentResponse> payments,
    Instant createdAt,
    Instant updatedAt
) {}
