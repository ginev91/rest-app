package org.example.main.exception;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Main application.
 * Handles:
 *  - validation errors (MethodArgumentNotValidException, ConstraintViolationException)
 *  - ResponseStatusException (preserves status and reason)
 *  - ResourceNotFoundException -> 404
 *  - FeignException -> 502 Bad Gateway (upstream failure)
 *  - generic Exception -> 500 Internal Server Error
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> body = Map.of(
                "message", "Validation failed",
                "errors", errors
        );
        log.debug("Validation failed: {}", errors);
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a
                ));
        Map<String, Object> body = Map.of(
                "message", "Validation failed",
                "errors", errors
        );
        log.debug("Constraint violations: {}", errors);
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = Map.of(
                "message", ex.getReason(),
                "status", ex.getStatusCode().value()
        );
        log.info("ResponseStatusException: {} {}", ex.getStatusCode(), ex.getReason());
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    // Custom application exception handler -> ensures there's at least one custom exception handler in Main
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "message", ex.getMessage(),
                "status", HttpStatus.NOT_FOUND.value()
        );
        log.info("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Handle Feign client errors (upstream microservice failures)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignException(FeignException ex) {
        String details = "";
        try {
            details = ex.contentUTF8();
        } catch (Exception ignored) { }
        Map<String, Object> body = Map.of(
                "message", "Upstream service error",
                "status", ex.status() > 0 ? ex.status() : HttpStatus.BAD_GATEWAY.value(),
                "details", details
        );
        log.warn("FeignException: status={} body={}", ex.status(), details);
        return new ResponseEntity<>(body, HttpStatus.BAD_GATEWAY);
    }

    // Generic fallback - avoids white-label pages
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        Map<String, Object> body = Map.of(
                "message", "Internal server error",
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}