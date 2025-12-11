package org.example.main.exception;

import feign.FeignException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // helper method used to build a MethodParameter for MethodArgumentNotValidException
    @SuppressWarnings("unused")
    private void dummyParamMethod(String ignored) { /* used for MethodParameter reflection */ }

    @Test
    void handleValidationException_buildsBadRequestWithErrors() throws NoSuchMethodException {
        Method m = this.getClass().getDeclaredMethod("dummyParamMethod", String.class);
        MethodParameter mp = new MethodParameter(m, 0);

        // create a BindingResult with a FieldError
        BindingResult br = new BeanPropertyBindingResult(new Object(), "target");
        FieldError fe = new FieldError("target", "name", "must not be blank");
        br.addError(fe);

        org.springframework.web.bind.MethodArgumentNotValidException ex =
                new org.springframework.web.bind.MethodArgumentNotValidException(mp, br);

        ResponseEntity<Object> resp = handler.handleValidationException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Validation failed");
        assertThat(body).containsKey("errors");

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertThat(errors).containsEntry("name", "must not be blank");
    }

    @Test
    void handleConstraintViolation_buildsBadRequestWithErrors() {
        // mock a ConstraintViolation
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> cv = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("entity.field");
        when(cv.getPropertyPath()).thenReturn(path);
        when(cv.getMessage()).thenReturn("invalid");
        Set<ConstraintViolation<Object>> set = Collections.singleton((ConstraintViolation<Object>) cv);

        ConstraintViolationException ex = new ConstraintViolationException(set);

        ResponseEntity<Object> resp = handler.handleConstraintViolation(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Validation failed");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertThat(errors).containsEntry("entity.field", "invalid");
    }

    @Test
    void handleResponseStatusException_returnsStatusAndReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "teapot");
        ResponseEntity<Object> resp = handler.handleResponseStatusException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "teapot");
        assertThat(body).containsEntry("status", HttpStatus.I_AM_A_TEAPOT.value());
    }

    @Test
    void handleResourceNotFound_returnsNotFound() {
        // assume ResourceNotFoundException exists with a String ctor
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");
        ResponseEntity<Object> resp = handler.handleResourceNotFound(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "not found");
        assertThat(body).containsEntry("status", HttpStatus.NOT_FOUND.value());
    }

    @Test
    void handleFeignException_includesStatusAndDetails_whenContentAvailable() {
        FeignException fe = mock(FeignException.class);
        when(fe.status()).thenReturn(502);
        when(fe.contentUTF8()).thenReturn("upstream body");

        ResponseEntity<Object> resp = handler.handleFeignException(fe);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Upstream service error");
        assertThat(body).containsEntry("status", 502);
        assertThat(body).containsEntry("details", "upstream body");
    }

    @Test
    void handleFeignException_handlesContentFailure_gracefully() {
        FeignException fe = mock(FeignException.class);
        when(fe.status()).thenReturn(-1);
        when(fe.contentUTF8()).thenThrow(new RuntimeException("oops"));

        ResponseEntity<Object> resp = handler.handleFeignException(fe);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Upstream service error");
        // status should fallback to BAD_GATEWAY.value() when Feign status <= 0
        assertThat(body).containsEntry("status", HttpStatus.BAD_GATEWAY.value());
        // details is present (empty string expected)
        assertThat(body).containsKey("details");
    }

    @Test
    void handleAuthenticationException_returnsUnauthorized() {
        AuthenticationException ex = new AuthenticationException("bad auth") {};
        ResponseEntity<Object> resp = handler.handleAuthenticationException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Unauthenticated");
        assertThat(body).containsEntry("detail", "bad auth");
        assertThat(body).containsEntry("status", HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("nope");
        ResponseEntity<Object> resp = handler.handleAccessDenied(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Access denied");
        assertThat(body).containsEntry("detail", "nope");
        assertThat(body).containsEntry("status", HttpStatus.FORBIDDEN.value());
    }

    @Test
    void handleOther_returnsInternalServerError() {
        RuntimeException ex = new RuntimeException("boom");
        ResponseEntity<Object> resp = handler.handleOther(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Internal server error");
        assertThat(body).containsEntry("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}