package com.severus.finance.configuration;

import com.severus.finance.settings.AppSettingsService;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class StartupInitializer {

    private static final Logger LOG = Logger.getLogger(StartupInitializer.class);

    @Inject
    UserRepository userRepository;

    @Inject
    AppSettingsService appSettingsService;

    @Inject
    AppConfig appConfig;

    @Inject
    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    int httpPort;

    @Transactional
    public void onStartup(@Observes StartupEvent ev) {
        LOG.info("Initializing Severus Finance application backend...");

        String email = appConfig.singleUser().email().toLowerCase().trim();
        String rawPassword = appConfig.singleUser().password();

        String passwordHash = BcryptUtil.bcryptHash(rawPassword, 12);
        userRepository.findByEmail(email).ifPresentOrElse(
            user -> {
                if (!BcryptUtil.matches(rawPassword, user.getPasswordHash())) {
                    LOG.info("Syncing single user password from environment configuration...");
                    user.setPasswordHash(passwordHash);
                    user.setUpdatedAt(java.time.Instant.now());
                    userRepository.persist(user);
                    LOG.info("Single user password updated successfully.");
                } else {
                    LOG.info("User already exists with matching credentials. Skipping seed.");
                }
            },
            () -> {
                LOG.infof("Seeding single user with email: %s", email);
                UUID userId = UUID.randomUUID();
                User user = new User(userId, email, passwordHash);
                userRepository.persist(user);
                appSettingsService.createDefaultSettings(userId);
                LOG.info("Single user seeded successfully.");
            }
        );

        LOG.infof("\n=================================================\n" +
                 "✅ Severus Finance Backend is RUNNING\n" +
                 "API:      http://localhost:%d\n" +
                 "Health:   http://localhost:%d/q/health\n" +
                 "Database: PostgreSQL connected\n" +
                 "Frontend: http://localhost:5173\n" +
                 "=================================================", httpPort, httpPort);
    }
}
