package com.severus.finance.todo;

import com.severus.finance.security.SecurityUtils;
import com.severus.finance.todo.dto.CreateTodoRequest;
import com.severus.finance.todo.dto.TodoResponse;
import com.severus.finance.todo.dto.UpdateTodoRequest;
import com.severus.finance.todo.enums.TodoStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/todos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TodoItemResource {

    @Inject
    TodoItemService todoService;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getTodos(@QueryParam("status") TodoStatus status) {
        UUID userId = SecurityUtils.getUserId(jwt);
        List<TodoResponse> list = todoService.getTodos(userId, status);
        return Response.ok(list).build();
    }

    @POST
    public Response createTodo(@Valid CreateTodoRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        TodoResponse response = todoService.createTodo(userId, request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateTodo(@PathParam("id") UUID id, @Valid UpdateTodoRequest request) {
        UUID userId = SecurityUtils.getUserId(jwt);
        TodoResponse response = todoService.updateTodo(id, userId, request);
        return Response.ok(response).build();
    }

    @PATCH
    @Path("/{id}/toggle")
    public Response toggleTodo(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        TodoResponse response = todoService.toggleStatus(id, userId);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTodo(@PathParam("id") UUID id) {
        UUID userId = SecurityUtils.getUserId(jwt);
        todoService.deleteTodo(id, userId);
        return Response.noContent().build();
    }
}
