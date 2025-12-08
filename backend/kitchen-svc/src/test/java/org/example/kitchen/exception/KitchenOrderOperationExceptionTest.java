package org.example.kitchen.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KitchenOrderOperationExceptionTest {

    @Test
    void constructor_setsMessage_only() {
        String message = "Order is already CANCELLED";
        KitchenOrderOperationException ex = new KitchenOrderOperationException(message);

        assertEquals(message, ex.getMessage());
        assertNull(ex.getCause(), "Cause should be null when not provided");
    }

    @Test
    void constructor_setsMessageAndCause() {
        String message = "Failed to modify order";
        Throwable cause = new IllegalStateException("state error");

        KitchenOrderOperationException ex = new KitchenOrderOperationException(message, cause);

        assertEquals(message, ex.getMessage());
        assertSame(cause, ex.getCause(), "Cause should be the same instance passed to the constructor");
    }

    @Test
    void exception_isRuntimeException() {
        KitchenOrderOperationException ex = new KitchenOrderOperationException("runtime check");
        assertTrue(ex instanceof RuntimeException);
    }
}