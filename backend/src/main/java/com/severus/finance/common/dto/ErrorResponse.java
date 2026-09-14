package com.severus.finance.common.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String errorCode,
    String message,
    String path,
    Map<String, String> fieldErrors,
    String correlationId
) {
    public static ErrorResponse of(int status, String errorCode, String message, String path, Map<String, String> fieldErrors, String correlationId) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, path, fieldErrors, correlationId);
    }
}
