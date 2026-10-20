package com.severus.finance.common;

import com.severus.finance.common.dto.ErrorResponse;
import com.severus.finance.common.exception.GlobalExceptionMapper;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionMapperTest {

    @Test
    @DisplayName("NotFoundException should map to 404 NOT_FOUND ErrorResponse")
    public void testNotFoundExceptionMapping() {
        GlobalExceptionMapper mapper = new GlobalExceptionMapper();
        NotFoundException exception = new NotFoundException("HTTP 404 Not Found");

        Response response = mapper.toResponse(exception);

        assertEquals(404, response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) response.getEntity();
        assertEquals(404, error.status());
        assertEquals("NOT_FOUND", error.errorCode());
        assertNotNull(error.correlationId());
    }
}

