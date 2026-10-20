package com.severus.finance.common.exception;

import com.severus.finance.common.dto.ErrorResponse;
import jakarta.annotation.Priority;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Provider
@Priority(Priorities.USER)
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api/v1";
        String correlationId = UUID.randomUUID().toString();

        if (exception instanceof ApiException apiEx) {
            LOG.warnf("API Exception [%s]: %s (path: %s)", apiEx.getErrorCode(), apiEx.getMessage(), path);
            ErrorResponse errorResponse = ErrorResponse.of(
                apiEx.getStatus().getStatusCode(),
                apiEx.getErrorCode(),
                apiEx.getMessage(),
                path,
                apiEx.getFieldErrors(),
                correlationId
            );
            return Response.status(apiEx.getStatus()).entity(errorResponse).build();
        }

        if (exception instanceof ConstraintViolationException cve) {
            Map<String, String> errors = new HashMap<>();
            cve.getConstraintViolations().forEach(violation -> {
                String field = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                errors.put(field, message);
            });
            LOG.warnf("Validation exception: %s (path: %s)", errors, path);
            ErrorResponse errorResponse = ErrorResponse.of(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "VALIDATION_ERROR",
                "Validation failed for request",
                path,
                errors,
                correlationId
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        if (exception instanceof OptimisticLockException) {
            LOG.warnf("Optimistic lock failure: %s (path: %s)", exception.getMessage(), path);
            ErrorResponse errorResponse = ErrorResponse.of(
                Response.Status.CONFLICT.getStatusCode(),
                "CONCURRENT_MODIFICATION",
                "The record was modified by another transaction. Please reload and try again.",
                path,
                null,
                correlationId
            );
            return Response.status(Response.Status.CONFLICT).entity(errorResponse).build();
        }

        if (exception instanceof WebApplicationException webAppEx) {
            int status = webAppEx.getResponse() != null ? webAppEx.getResponse().getStatus() : 500;
            String message = webAppEx.getMessage();
            if (status == Response.Status.NOT_FOUND.getStatusCode()) {
                LOG.debugf("Resource not found: %s", path);
                ErrorResponse errorResponse = ErrorResponse.of(
                    Response.Status.NOT_FOUND.getStatusCode(),
                    "NOT_FOUND",
                    "Resource not found: " + path,
                    path,
                    null,
                    correlationId
                );
                return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
            }

            if (status < 500) {
                LOG.warnf("Web application exception [%d]: %s (path: %s)", status, message, path);
            } else {
                LOG.errorf(webAppEx, "Web application exception [%d] (path: %s): %s", status, path, message);
            }

            ErrorResponse errorResponse = ErrorResponse.of(
                status,
                "HTTP_" + status,
                message != null ? message : "HTTP error " + status,
                path,
                null,
                correlationId
            );
            return Response.status(status).entity(errorResponse).build();
        }

        LOG.errorf(exception, "Unhandled Exception [ID: %s]: %s (path: %s)", correlationId, exception.getMessage(), path);
        ErrorResponse errorResponse = ErrorResponse.of(
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "INTERNAL_SERVER_ERROR",
            "An unexpected internal error occurred",
            path,
            null,
            correlationId
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
    }
}
