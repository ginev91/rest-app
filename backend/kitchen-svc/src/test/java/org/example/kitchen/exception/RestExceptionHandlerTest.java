package org.example.kitchen.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RestExceptionHandlerTest {

    @Test
    void handleNotFound_returnsNotFoundAndErrorBody() {
        RestExceptionHandler handler = new RestExceptionHandler();
        IllegalArgumentException ex = new IllegalArgumentException("resource not found");

        ResponseEntity<Map<String, String>> resp = handler.handleNotFound(ex);

        assertNotNull(resp);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("resource not found", resp.getBody().get("error"));
    }

    @Test
    void handleGeneric_returnsInternalServerErrorAndErrorBody() {
        RestExceptionHandler handler = new RestExceptionHandler();
        Exception ex = new Exception("something went wrong");

        ResponseEntity<Map<String, String>> resp = handler.handleGeneric(ex);

        assertNotNull(resp);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("something went wrong", resp.getBody().get("error"));
    }
}