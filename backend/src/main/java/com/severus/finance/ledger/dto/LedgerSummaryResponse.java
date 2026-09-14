package com.severus.finance.ledger.dto;

import java.math.BigDecimal;

public record LedgerSummaryResponse(
    BigDecimal totalLentReceivable,
    BigDecimal totalBorrowedPayable,
    long openLentCount,
    long openBorrowedCount
) {}
