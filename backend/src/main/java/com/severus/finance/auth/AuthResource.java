package com.severus.finance.auth;

import com.severus.finance.auth.dto.ChangePasswordRequest;
import com.severus.finance.auth.dto.LoginRequest;
import com.severus.finance.auth.dto.LoginResponse;
import com.severus.finance.auth.dto.LoginResult;
import com.severus.finance.auth.dto.UserResponse;
import com.severus.finance.security.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request, @HeaderParam("X-Forwarded-For") String forwardedFor) {
        String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "127.0.0.1";
        LoginResult result = authService.login(request, clientIp);

        NewCookie refreshCookie = createRefreshCookie(result.rawRefreshToken(), 7 * 24 * 3600);
        LoginResponse responseBody = new LoginResponse(
                result.accessToken(),
                result.expiresInSeconds(),
                result.userId(),
                result.email(),
                result.displayName()
        );

        return Response.ok(responseBody).cookie(refreshCookie).build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Response refresh(@CookieParam(REFRESH_COOKIE_NAME) Cookie cookie) {
        String rawRefreshToken = cookie != null ? cookie.getValue() : null;
        LoginResult result = authService.refresh(rawRefreshToken);

        NewCookie refreshCookie = createRefreshCookie(result.rawRefreshToken(), 7 * 24 * 3600);
        LoginResponse responseBody = new LoginResponse(
                result.accessToken(),
                result.expiresInSeconds(),
                result.userId(),
                result.email(),
                result.displayName()
        );

        return Response.ok(responseBody).cookie(refreshCookie).build();
    }

    @POST
    @Path("/logout")
    @PermitAll
    public Response logout(@CookieParam(REFRESH_COOKIE_NAME) Cookie cookie) {
        String rawRefreshToken = cookie != null ? cookie.getValue() : null;
        authService.logout(rawRefreshToken);

        NewCookie clearCookie = createRefreshCookie("", 0);
        return Response.ok().cookie(clearCookie).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed("USER")
    public Response getCurrentUser() {
        UUID userId = SecurityUtils.getUserId(jwt);
        UserResponse user = authService.getCurrentUser(userId);
        return Response.ok(user).build();
    }

    @POST
    @Path("/change-password")
    @RolesAllowed("USER")
    public Response changePassword(@Valid ChangePasswordRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        authService.changePassword(userId, request);

        NewCookie clearCookie = createRefreshCookie("", 0);
        return Response.ok().cookie(clearCookie).build();
    }

    private NewCookie createRefreshCookie(String value, int maxAge) {
        return new NewCookie.Builder(REFRESH_COOKIE_NAME)
                .value(value)
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .secure(false)
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }
}
