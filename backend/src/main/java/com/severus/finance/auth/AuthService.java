package com.severus.finance.auth;

import com.severus.finance.auth.dto.ChangePasswordRequest;
import com.severus.finance.auth.dto.LoginRequest;
import com.severus.finance.auth.dto.LoginResult;
import com.severus.finance.auth.dto.UserResponse;
import com.severus.finance.common.exception.ApiException;
import com.severus.finance.security.JwtService;
import com.severus.finance.security.RateLimiter;
import com.severus.finance.settings.AppSettings;
import com.severus.finance.settings.AppSettingsRepository;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS_MSG = "Invalid email or password";
    private static final int REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppSettingsRepository appSettingsRepository;
    private final JwtService jwtService;
    private final RateLimiter rateLimiter;

    @Inject
    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            AppSettingsRepository appSettingsRepository,
            JwtService jwtService,
            RateLimiter rateLimiter
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.appSettingsRepository = appSettingsRepository;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public LoginResult login(LoginRequest request, String clientIp) {
        String rateLimitKey = clientIp + ":" + request.email().toLowerCase().trim();
        if (rateLimiter.isBlocked(rateLimitKey)) {
            LOG.warnf("Rate limit exceeded for login attempt: %s", rateLimitKey);
            throw new ApiException(Response.Status.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many failed login attempts. Please try again later.");
        }

        Optional<User> userOpt = userRepository.findByEmail(request.email().toLowerCase().trim());
        if (userOpt.isEmpty()) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            throw new ApiException(Response.Status.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_CREDENTIALS_MSG);
        }

        User user = userOpt.get();
        if (!BcryptUtil.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            throw new ApiException(Response.Status.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_CREDENTIALS_MSG);
        }

        rateLimiter.reset(rateLimitKey);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = jwtService.generateRefreshToken();
        String refreshTokenHash = jwtService.hashRefreshToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);
        RefreshToken refreshTokenEntity = new RefreshToken(UUID.randomUUID(), user, refreshTokenHash, expiresAt);
        refreshTokenRepository.persist(refreshTokenEntity);

        String displayName = appSettingsRepository.findByUserId(user.getId())
                .map(AppSettings::getDisplayName)
                .orElse("Severus");

        return new LoginResult(accessToken, 900L, user.getId(), user.getEmail(), displayName, rawRefreshToken);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "INVALID_TOKEN", "Refresh token missing");
        }

        String tokenHash = jwtService.hashRefreshToken(rawRefreshToken);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "INVALID_TOKEN", "Invalid refresh token");
        }

        RefreshToken token = tokenOpt.get();
        if (token.isRevoked()) {
            LOG.warnf("SECURITY ALERT: Revoked refresh token reuse attempt for user: %s. Revoking all tokens!", token.getUser().getEmail());
            refreshTokenRepository.revokeAllForUser(token.getUser().getId());
            throw new ApiException(Response.Status.UNAUTHORIZED, "TOKEN_REVOKED", "Security warning: Revoked token presented");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevoked(true);
            throw new ApiException(Response.Status.UNAUTHORIZED, "TOKEN_EXPIRED", "Refresh token expired");
        }

        // Rotate token
        token.setRevoked(true);

        User user = token.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String newRawRefreshToken = jwtService.generateRefreshToken();
        String newRefreshTokenHash = jwtService.hashRefreshToken(newRawRefreshToken);

        Instant newExpiresAt = Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);
        RefreshToken newRefreshTokenEntity = new RefreshToken(UUID.randomUUID(), user, newRefreshTokenHash, newExpiresAt);
        refreshTokenRepository.persist(newRefreshTokenEntity);

        String displayName = appSettingsRepository.findByUserId(user.getId())
                .map(AppSettings::getDisplayName)
                .orElse("Severus");

        return new LoginResult(newAccessToken, 900L, user.getId(), user.getEmail(), displayName, newRawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = jwtService.hashRefreshToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> token.setRevoked(true));
        }
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (!BcryptUtil.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(Response.Status.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }

        String newPasswordHash = BcryptUtil.bcryptHash(request.newPassword(), 12);
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(Instant.now());

        // Revoke all refresh sessions
        refreshTokenRepository.revokeAllForUser(userId);
    }

    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String displayName = appSettingsRepository.findByUserId(user.getId())
                .map(AppSettings::getDisplayName)
                .orElse("Severus");

        return new UserResponse(user.getId(), user.getEmail(), displayName);
    }
}
