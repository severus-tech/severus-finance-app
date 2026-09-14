package com.severus.finance.auth.dto;

import java.util.UUID;

public record LoginResponse(
    String accessToken,
    long expiresInSeconds,
    UUID userId,
    String email,
    String displayName
) {}
