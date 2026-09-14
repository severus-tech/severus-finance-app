package com.severus.finance.settings;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AppSettingsRepository implements PanacheRepositoryBase<AppSettings, UUID> {

    public Optional<AppSettings> findByUserId(UUID userId) {
        return find("user.id", userId).firstResultOptional();
    }
}
