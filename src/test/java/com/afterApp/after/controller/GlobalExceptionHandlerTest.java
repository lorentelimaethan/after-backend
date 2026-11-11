package com.afterApp.after.controller;

import com.afterApp.after.dto.ErrorResponseDTO;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.FormatRequestException;
import com.afterApp.after.exceptions.InvalidTokenException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Calls the handler methods directly and checks the status code and body of each error. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");

    @Test
    void shouldReturn400ForBadRequestException() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleBadRequest(new BadRequestException("bad"), request);

        checkResponse(response, 400, "bad");
    }

    @Test
    void shouldReturn400ForFormatRequestException() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleBadRequest(new FormatRequestException("format"), request);

        checkResponse(response, 400, "format");
    }

    @Test
    void shouldReturn401WithGenericMessageForInvalidToken() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleInvalidToken(new InvalidTokenException("secret"), request);

        checkResponse(response, 401, "Access denied");
    }

    @Test
    void shouldReturn403ForUnauthorizedException() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleUnauthorized(new UnauthorizedException("forbidden"), request);

        checkResponse(response, 403, "forbidden");
    }

    @Test
    void shouldReturn403WithGenericMessageForAccessDenied() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleAccessDenied(new AccessDeniedException("secret"), request);

        checkResponse(response, 403, "Access denied");
    }

    @Test
    void shouldReturn404WithGenericMessageForNotFound() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleNotFound(new NotFoundException("secret"), request);

        checkResponse(response, 404, "Content not Found");
    }

    @Test
    void shouldReturn409ForAlreadyExists() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleAlreadyExists(new AlreadyExistsException("duplicate"), request);

        checkResponse(response, 409, "duplicate");
    }

    @Test
    void shouldReturn500WithoutInternalDetails() {
        ResponseEntity<ErrorResponseDTO> response =
                handler.handleInternalServerError(new RuntimeException("private details"), request);

        checkResponse(response, 500, "Unexpected Server Error");
    }

    private void checkResponse(ResponseEntity<ErrorResponseDTO> response, int status, String message) {
        assertEquals(status, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(status, response.getBody().getStatus());
        assertEquals(message, response.getBody().getMessage());
        assertEquals("/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }
}
