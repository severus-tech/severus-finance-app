package com.severus.finance.security;

import com.severus.finance.common.exception.ApiException;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class JwtService {

    private final String jwtSecret;
    private final String issuer;
    private final SecretKey secretKey;
    private final JWTAuthContextInfo authContextInfo;

    @Inject
    public JwtService(
            @ConfigProperty(name = "smallrye.jwt.sign.key") String jwtSecret,
            @ConfigProperty(name = "smallrye.jwt.token.issuer", defaultValue = "https://severus-finance.com") String issuer
    ) {
        if (jwtSecret == null || jwtSecret.trim().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET environment variable is missing or less than 32 bytes!");
        }
        this.jwtSecret = jwtSecret.trim();
        this.issuer = issuer;
        this.secretKey = new SecretKeySpec(this.jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.authContextInfo = new JWTAuthContextInfo(this.secretKey, this.issuer);
        this.authContextInfo.setSignatureAlgorithm(SignatureAlgorithm.HS256);
    }

    public String generateAccessToken(UUID userId, String email) {
        return Jwt.issuer(issuer)
                .subject(email)
                .claim("userId", userId.toString())
                .groups(Set.of("USER"))
                .expiresIn(Duration.ofMinutes(15))
                .signWithSecret(jwtSecret);
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    public JsonWebToken verifyToken(String token) {
        try {
            return DefaultJWTCallerPrincipalFactory.instance().parse(token, authContextInfo);
        } catch (ParseException e) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "INVALID_TOKEN", "Token verification failed: " + e.getMessage());
        }
    }

    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, "CRYPTO_ERROR", "SHA-256 algorithm unavailable");
        }
    }
}
