package com.severus.finance.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AppSettingsRequest(
    @NotNull(message = "Monthly salary is required")
    @Min(value = 0, message = "Salary cannot be negative")
    BigDecimal monthlySalary,

    @Min(value = 1, message = "Salary credit day must be between 1 and 31")
    @Max(value = 31, message = "Salary credit day must be between 1 and 31")
    int salaryCreditDay,

    @NotBlank(message = "Currency is required")
    String currency,

    @NotBlank(message = "Timezone is required")
    String timezone,

    @NotBlank(message = "Display name is required")
    String displayName,

    @NotBlank(message = "Theme preference is required")
    String themePreference
) {}
