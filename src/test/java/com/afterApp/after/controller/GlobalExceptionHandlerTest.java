package com.afterApp.after.controller;

import com.afterApp.after.dto.ErrorResponseDTO;
import com.afterApp.after.exceptions.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");

    @Test
    void shouldMapDomainErrorsAndHideInternalDetails() {
        check(handler.handleBadRequest(new BadRequestException("bad"), request), 400, "bad");
        check(handler.handleBadRequest(new FormatRequestException("format"), request), 400, "format");
        check(handler.handleInvalidToken(new InvalidTokenException("secret"), request), 401, "Access denied");
        check(handler.handleUnauthorized(new UnauthorizedException("forbidden"), request), 403, "forbidden");
        check(handler.handleAccessDenied(new AccessDeniedException("secret"), request), 403, "Access denied");
        check(handler.handleNotFound(new NotFoundException("secret"), request), 404, "Content not Found");
        check(handler.handleAlreadyExists(new AlreadyExistsException("duplicate"), request), 409, "duplicate");
        check(handler.handleInternalServerError(new RuntimeException("private details"), request), 500, "Unexpected Server Error");
    }

    private void check(ResponseEntity<ErrorResponseDTO> response, int status, String message) {
        assertEquals(status, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(status, response.getBody().getStatus());
        assertEquals(message, response.getBody().getMessage());
        assertEquals("/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }
}
