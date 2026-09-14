package com.severus.finance.expense;

import com.severus.finance.expense.dto.*;
import com.severus.finance.expense.enums.ExpenseType;
import com.severus.finance.loan.enums.PaymentState;
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

@Path("/api/v1/expenses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ExpenseResource {

    @Inject
    ExpenseService expenseService;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getExpenses(@QueryParam("type") ExpenseType type) {
        UUID userId = SecurityUtils.getUserId(jwt);
        List<ExpenseResponse> list = expenseService.getExpenses(userId, type);
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    public Response getExpense(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        ExpenseResponse expense = expenseService.getExpense(id, userId);
        return Response.ok(expense).build();
    }

    @POST
    public Response createExpense(@Valid CreateExpenseRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        ExpenseResponse response = expenseService.createExpense(userId, request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateExpense(@PathParam("id") UUID id, @Valid UpdateExpenseRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        ExpenseResponse response = expenseService.updateExpense(id, userId, request);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteExpense(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        expenseService.deleteExpense(id, userId);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}/status")
    public Response togglePaymentState(@PathParam("id") UUID id, @QueryParam("state") PaymentState state) {
        UUID userId = SecurityUtils.getUserId(jwt);
        ExpenseResponse response = expenseService.toggleExpensePaymentState(id, userId, state != null ? state : PaymentState.PAID);
        return Response.ok(response).build();
    }
}
