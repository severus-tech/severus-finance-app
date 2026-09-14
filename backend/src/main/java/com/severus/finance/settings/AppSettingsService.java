package com.severus.finance.settings;

import com.severus.finance.common.exception.ApiException;
import com.severus.finance.common.util.CurrencyUtils;
import com.severus.finance.settings.dto.AppSettingsRequest;
import com.severus.finance.settings.dto.AppSettingsResponse;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository;
    private final UserRepository userRepository;

    @Inject
    public AppSettingsService(AppSettingsRepository appSettingsRepository, UserRepository userRepository) {
        this.appSettingsRepository = appSettingsRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AppSettingsResponse getSettings(UUID userId) {
        AppSettings settings = appSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return toResponse(settings);
    }

    @Transactional
    public AppSettingsResponse updateSettings(UUID userId, AppSettingsRequest request) {
        AppSettings settings = appSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.setMonthlySalary(CurrencyUtils.format(request.monthlySalary()));
        settings.setSalaryCreditDay(request.salaryCreditDay());
        if (request.currency() != null && !request.currency().isBlank()) {
            settings.setCurrency(request.currency());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            settings.setTimezone(request.timezone());
        }
        settings.setDisplayName(request.displayName());
        if (request.themePreference() != null && !request.themePreference().isBlank()) {
            settings.setThemePreference(request.themePreference());
        }
        settings.setUpdatedAt(Instant.now());

        return toResponse(settings);
    }

    @Transactional
    public AppSettings createDefaultSettings(UUID userId) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        AppSettings settings = new AppSettings(
                UUID.randomUUID(),
                user,
                CurrencyUtils.format(new java.math.BigDecimal("80000.00")),
                1,
                "INR",
                "Asia/Kolkata",
                "Severus",
                "DARK"
        );
        appSettingsRepository.persist(settings);
        return settings;
    }

    public AppSettingsResponse toResponse(AppSettings s) {
        return new AppSettingsResponse(
                s.getMonthlySalary(),
                s.getSalaryCreditDay(),
                s.getCurrency(),
                s.getTimezone(),
                s.getDisplayName(),
                s.getThemePreference(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
