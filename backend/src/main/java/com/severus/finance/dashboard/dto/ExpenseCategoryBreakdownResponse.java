package com.severus.finance.dashboard.dto;

import java.math.BigDecimal;

public record ExpenseCategoryBreakdownResponse(
    String category,
    BigDecimal totalAmount,
    double percentage
) {}
