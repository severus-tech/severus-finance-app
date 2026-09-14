package com.severus.finance.ledger;

import com.severus.finance.ledger.dto.*;
import com.severus.finance.ledger.enums.LedgerDirection;
import com.severus.finance.ledger.enums.LedgerStatus;
import com.severus.finance.security.SecurityUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/ledger")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class LedgerResource {

    @Inject
    LedgerService ledgerService;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getLedgerEntries(
            @QueryParam("direction") LedgerDirection direction,
            @QueryParam("status") LedgerStatus status
    ) {
        UUID userId = SecurityUtils.getUserId(jwt);
        List<LedgerEntryResponse> list = ledgerService.getLedgerEntries(userId, direction, status);
        return Response.ok(list).build();
    }

    @GET
    @Path("/summary")
    public Response getLedgerSummary() {
        UUID userId = SecurityUtils.getUserId(jwt);
        LedgerSummaryResponse summary = ledgerService.getLedgerSummary(userId);
        return Response.ok(summary).build();
    }

    @GET
    @Path("/{id}")
    public Response getLedgerEntry(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LedgerEntryResponse entry = ledgerService.getLedgerEntry(id, userId);
        return Response.ok(entry).build();
    }

    @POST
    public Response createLedgerEntry(@Valid CreateLedgerEntryRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LedgerEntryResponse response = ledgerService.createLedgerEntry(userId, request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateLedgerEntry(@PathParam("id") UUID id, @Valid UpdateLedgerEntryRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LedgerEntryResponse response = ledgerService.updateLedgerEntry(id, userId, request);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteLedgerEntry(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        ledgerService.deleteLedgerEntry(id, userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/payments")
    public Response recordPayment(@PathParam("id") UUID id, @Valid RecordLedgerPaymentRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LedgerEntryResponse response = ledgerService.recordPayment(id, userId, request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/{id}/reopen")
    public Response reopenEntry(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LedgerEntryResponse response = ledgerService.reopenEntry(id, userId);
        return Response.ok(response).build();
    }
}
