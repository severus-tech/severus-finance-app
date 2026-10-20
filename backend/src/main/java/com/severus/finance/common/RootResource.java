package com.severus.finance.common;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;

@Path("/")
@ApplicationScoped
public class RootResource {

    @GET
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Response root() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "Severus Finance Backend",
            "version", "1.0.0-SNAPSHOT",
            "timestamp", Instant.now().toString(),
            "healthCheck", "/q/health"
        )).build();
    }

    @GET
    @Path("/health")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        )).build();
    }
}

