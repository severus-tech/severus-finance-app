package com.severus.finance.settings;

import com.severus.finance.security.SecurityUtils;
import com.severus.finance.settings.dto.AppSettingsRequest;
import com.severus.finance.settings.dto.AppSettingsResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/v1/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class AppSettingsResource {

    @Inject
    AppSettingsService settingsService;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getSettings() {
        UUID userId = SecurityUtils.getUserId(jwt);
        AppSettingsResponse response = settingsService.getSettings(userId);
        return Response.ok(response).build();
    }

    @PUT
    public Response updateSettings(@Valid AppSettingsRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        AppSettingsResponse response = settingsService.updateSettings(userId, request);
        return Response.ok(response).build();
    }
}
