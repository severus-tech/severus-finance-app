package com.severus.finance.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CycleExpenseTrendResponse(
    String cycleLabel,
    LocalDate cycleStart,
    LocalDate cycleEnd,
    BigDecimal totalExpenses,
    BigDecimal totalEmis,
    BigDecimal totalFixed,
    BigDecimal totalVariable
) {}
