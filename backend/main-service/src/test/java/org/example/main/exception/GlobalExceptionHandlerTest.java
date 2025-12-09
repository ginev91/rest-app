package org.example.main.exception;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static class Dummy {
        public void method(String p) { }
    }

    @Test
    void handleValidationException_buildsBadRequestBody() throws Exception {
        Method m = Dummy.class.getMethod("method", String.class);
        MethodParameter mp = new MethodParameter(m, 0);
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "obj");
        br.addError(new FieldError("obj", "name", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, br);
        var resp = handler.handleValidationException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Object body = resp.getBody();
        assertThat(body).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertThat(map.get("message")).isEqualTo("Validation failed");
        assertThat(map.get("errors")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) map.get("errors");
        assertThat(errors).containsEntry("name", "must not be blank");
    }

    @Test
    void handleConstraintViolation_convertsViolationsToMap() {
        ConstraintViolation<?> v = mock(ConstraintViolation.class);
        Path p = mock(Path.class);
        when(p.toString()).thenReturn("obj.field");
        when(v.getPropertyPath()).thenReturn(p);
        when(v.getMessage()).thenReturn("invalid");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(v));
        var resp = handler.handleConstraintViolation(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body.get("message")).isEqualTo("Validation failed");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertThat(errors).containsEntry("obj.field", "invalid");
    }

    @Test
    void handleResponseStatusException_returnsProvidedStatusAndReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        var resp = handler.handleResponseStatusException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body.get("message")).isEqualTo("not found");
        assertThat(body.get("status")).isEqualTo(404);
    }

    @Test
    void handleResourceNotFound_returnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("nope");
        var resp = handler.handleResourceNotFound(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body.get("message")).isEqualTo("nope");
        assertThat(body.get("status")).isEqualTo(404);
    }

    @Test
    void handleFeignException_includesStatusAndDetails() {
        FeignException fe = mock(FeignException.class);
        when(fe.status()).thenReturn(502);
        when(fe.contentUTF8()).thenReturn("{\"err\":\"upstream\"}");
        var resp = handler.handleFeignException(fe);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body.get("message")).isEqualTo("Upstream service error");
        assertThat(body.get("status")).isEqualTo(502);
        assertThat(body.get("details")).isEqualTo("{\"err\":\"upstream\"}");
    }

    @Test
    void handleOther_returnsInternalServerError() {
        Exception ex = new RuntimeException("boom");
        var resp = handler.handleOther(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body.get("message")).isEqualTo("Internal server error");
        assertThat(body.get("status")).isEqualTo(500);
    }
}