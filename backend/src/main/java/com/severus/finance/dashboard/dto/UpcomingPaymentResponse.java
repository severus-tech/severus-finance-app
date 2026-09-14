package com.severus.finance.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpcomingPaymentResponse(
    UUID id,
    String title,
    String type, // "LOAN_EMI", "FIXED_EXPENSE", "LEDGER_DUE"
    BigDecimal amount,
    LocalDate dueDate,
    boolean overdue,
    String categoryOrLender
) {}
