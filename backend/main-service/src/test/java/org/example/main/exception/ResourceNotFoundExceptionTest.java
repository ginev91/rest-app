package org.example.main.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void defaultConstructor_hasNullMessageAndNoCause() {
        ResourceNotFoundException e = new ResourceNotFoundException();
        assertThat(e).isInstanceOf(RuntimeException.class);
        assertThat(e.getMessage()).isNull();
        assertThat(e.getCause()).isNull();
    }

    @Test
    void messageConstructor_setsMessage() {
        ResourceNotFoundException e = new ResourceNotFoundException("not found");
        assertThat(e.getMessage()).isEqualTo("not found");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void messageAndCauseConstructor_setsBoth() {
        Throwable cause = new IllegalArgumentException("bad");
        ResourceNotFoundException e = new ResourceNotFoundException("not found", cause);
        assertThat(e.getMessage()).isEqualTo("not found");
        assertThat(e.getCause()).isSameAs(cause);
    }

    @Test
    void causeConstructor_setsCause_and_messageIsCauseToString() {
        Throwable cause = new IllegalStateException("state");
        ResourceNotFoundException e = new ResourceNotFoundException(cause);
        // Throwable(Throwable cause) sets message to cause == null ? null : cause.toString()
        assertThat(e.getCause()).isSameAs(cause);
        assertThat(e.getMessage()).isEqualTo(cause.toString());
    }
}