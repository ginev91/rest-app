package org.example.kitchen.exception;

import org.example.main.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RestExceptionHandlerTest {

    @Test
    void handleNotFound_returnsNotFoundAndErrorBody() {
        RestExceptionHandler handler = new RestExceptionHandler();
        ResourceNotFoundException ex = new ResourceNotFoundException("resource not found");

        ResponseEntity<Map<String, String>> resp = handler.handleNotFound(ex);

        assertNotNull(resp);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("resource not found", resp.getBody().get("error"));
    }

    @Test
    void handleBadRequest_forIllegalArgument_returnsBadRequestAndErrorBody() {
        RestExceptionHandler handler = new RestExceptionHandler();
        IllegalArgumentException ex = new IllegalArgumentException("invalid input");

        ResponseEntity<Map<String, String>> resp = handler.handleBadRequest(ex);

        assertNotNull(resp);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("invalid input", resp.getBody().get("error"));
    }

    @Test
    void handleGeneric_returnsInternalServerErrorAndGenericErrorBody() {
        RestExceptionHandler handler = new RestExceptionHandler();
        Exception ex = new Exception("something went wrong");

        ResponseEntity<Map<String, String>> resp = handler.handleGeneric(ex);

        assertNotNull(resp);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        // handler returns a generic message, do not assert original exception message
        assertEquals("Internal server error", resp.getBody().get("error"));
    }
}