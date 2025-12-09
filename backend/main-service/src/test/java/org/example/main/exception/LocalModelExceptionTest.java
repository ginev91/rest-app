package org.example.main.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LocalModelExceptionTest {

    @Test
    void messageConstructor_setsMessageAndNoCause() {
        LocalModelException ex = new LocalModelException("something went wrong");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("something went wrong");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void messageAndCauseConstructor_setsBoth() {
        Throwable cause = new IllegalStateException("root");
        LocalModelException ex = new LocalModelException("higher-level", cause);
        assertThat(ex.getMessage()).isEqualTo("higher-level");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}