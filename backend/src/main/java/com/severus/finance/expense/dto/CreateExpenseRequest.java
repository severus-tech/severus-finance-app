package com.severus.finance.expense.dto;

import com.severus.finance.expense.enums.ExpenseCategory;
import com.severus.finance.expense.enums.ExpenseType;
import com.severus.finance.loan.enums.PaymentState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
    @NotBlank(message = "Expense name is required")
    String name,

    @NotNull(message = "Expense category is required")
    ExpenseCategory category,

    @NotNull(message = "Expense type is required")
    ExpenseType type,

    @NotNull(message = "Expense amount is required")
    @Min(value = 0, message = "Amount must be non-negative")
    BigDecimal amount,

    @NotNull(message = "Expense date is required")
    LocalDate expenseDate,

    @Min(value = 1, message = "Monthly due day must be between 1 and 31")
    @Max(value = 31, message = "Monthly due day must be between 1 and 31")
    Integer monthlyDueDay,

    LocalDate startDate,
    LocalDate endDate,
    Boolean active,
    PaymentState paymentState,
    LocalDate paidDate,
    String notes
) {}
