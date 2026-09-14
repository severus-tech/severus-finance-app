package com.severus.finance.settings.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AppSettingsResponse(
    BigDecimal monthlySalary,
    int salaryCreditDay,
    String currency,
    String timezone,
    String displayName,
    String themePreference,
    Instant createdAt,
    Instant updatedAt
) {}
