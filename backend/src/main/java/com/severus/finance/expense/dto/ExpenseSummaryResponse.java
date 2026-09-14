package com.severus.finance.expense.dto;

import java.math.BigDecimal;

public record ExpenseSummaryResponse(
    BigDecimal totalFixedExpensesCycle,
    BigDecimal totalFixedExpensesDue,
    BigDecimal totalVariableExpensesCycle,
    BigDecimal totalExpensesCycle
) {}
