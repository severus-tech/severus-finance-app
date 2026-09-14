package com.severus.finance.dashboard;

import com.severus.finance.dashboard.dto.DashboardSummaryResponse;
import com.severus.finance.security.SecurityUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDate;
import java.util.UUID;

@Path("/api/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getDashboardSummary(@QueryParam("asOfDate") String asOfDateStr) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LocalDate asOfDate = null;

        if (asOfDateStr != null && !asOfDateStr.isBlank()) {
            try {
                asOfDate = LocalDate.parse(asOfDateStr);
            } catch (Exception e) {
                throw new BadRequestException("Invalid date format for asOfDate. Expected YYYY-MM-DD");
            }
        }

        DashboardSummaryResponse response = dashboardService.getDashboardSummary(userId, asOfDate);
        return Response.ok(response).build();
    }
}
