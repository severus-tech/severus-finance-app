package com.severus.finance.security;

import com.severus.finance.common.exception.ApiException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

public class SecurityUtils {

    public static UUID getUserId(JsonWebToken jwt) {
        if (jwt == null) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "UNAUTHORIZED", "Missing authentication JWT");
        }
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim == null) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "INVALID_TOKEN", "userId claim missing in token");
        }
        try {
            return UUID.fromString(userIdClaim.toString());
        } catch (IllegalArgumentException e) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "INVALID_TOKEN", "Invalid userId claim format");
        }
    }
}
