package com.severus.finance.auth.dto;

import java.util.UUID;

public record LoginResult(
    String accessToken,
    long expiresInSeconds,
    UUID userId,
    String email,
    String displayName,
    String rawRefreshToken
) {}
