package com.severus.finance.loan;

import com.severus.finance.loan.dto.*;
import com.severus.finance.loan.enums.LoanStatus;
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

@Path("/api/v1/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class LoanResource {

    @Inject
    LoanService loanService;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getLoans(@QueryParam("status") LoanStatus status) {
        UUID userId = SecurityUtils.getUserId(jwt);
        List<LoanResponse> loans = loanService.getLoans(userId, status);
        return Response.ok(loans).build();
    }

    @GET
    @Path("/summary")
    public Response getLoanSummary() {
        UUID userId = SecurityUtils.getUserId(jwt);
        LoanSummaryResponse summary = loanService.getLoanSummary(userId);
        return Response.ok(summary).build();
    }

    @GET
    @Path("/{id}")
    public Response getLoanDetails(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LoanResponse loan = loanService.getLoanDetails(id, userId);
        return Response.ok(loan).build();
    }

    @POST
    public Response createLoan(@Valid CreateLoanRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LoanResponse response = loanService.createLoan(userId, request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateLoan(@PathParam("id") UUID id, @Valid UpdateLoanRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LoanResponse response = loanService.updateLoan(id, userId, request);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteLoan(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        loanService.deleteLoan(id, userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/installments/{installmentId}/pay")
    public Response markInstallmentPaid(
            @PathParam("id") UUID id,
            @PathParam("installmentId") UUID installmentId,
            @Valid RecordPaymentRequest request
    ) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LoanResponse response = loanService.markInstallmentPaid(id, installmentId, userId, request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/{id}/installments/{installmentId}/undo")
    public Response undoInstallmentPayment(
            @PathParam("id") UUID id,
            @PathParam("installmentId") UUID installmentId
    ) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LoanResponse response = loanService.undoInstallmentPayment(id, installmentId, userId);
        return Response.ok(response).build();
    }

    @POST
    @Path("/{id}/prepayment")
    public Response recordPrepayment(
            @PathParam("id") UUID id,
            @Valid PrepaymentRequest request
    ) {
        UUID userId = SecurityUtils.getUserId(jwt);
        LoanResponse response = loanService.recordPrepayment(id, userId, request);
        return Response.ok(response).build();
    }
}
