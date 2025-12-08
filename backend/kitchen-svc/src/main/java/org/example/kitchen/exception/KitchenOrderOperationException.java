package org.example.kitchen.exception;

/**
 * Domain exception thrown when an operation on a KitchenOrder is not allowed
 * (e.g. attempting to modify a CANCELLED order).
 */
public class KitchenOrderOperationException extends RuntimeException {
    public KitchenOrderOperationException(String message) {
        super(message);
    }

    public KitchenOrderOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}