package com.severus.finance.common.exception;

import jakarta.ws.rs.core.Response;
import java.util.Map;

public class ApiException extends RuntimeException {
    private final Response.Status status;
    private final String errorCode;
    private final Map<String, String> fieldErrors;

    public ApiException(Response.Status status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.fieldErrors = null;
    }

    public ApiException(Response.Status status, String errorCode, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
