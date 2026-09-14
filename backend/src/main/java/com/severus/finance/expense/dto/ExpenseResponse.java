package com.severus.finance.expense.dto;

import com.severus.finance.expense.enums.ExpenseCategory;
import com.severus.finance.expense.enums.ExpenseType;
import com.severus.finance.loan.enums.PaymentState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    String name,
    ExpenseCategory category,
    ExpenseType type,
    BigDecimal amount,
    LocalDate expenseDate,
    Integer monthlyDueDay,
    LocalDate startDate,
    LocalDate endDate,
    boolean active,
    PaymentState paymentState,
    LocalDate paidDate,
    String notes,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
