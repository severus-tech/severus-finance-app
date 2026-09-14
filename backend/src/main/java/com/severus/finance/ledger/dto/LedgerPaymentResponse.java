package com.severus.finance.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerPaymentResponse(
    UUID id,
    UUID ledgerEntryId,
    BigDecimal amount,
    LocalDate paymentDate,
    String notes,
    Instant createdAt
) {}
