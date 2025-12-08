package org.example.kitchen.exception;


public class KitchenOrderOperationException extends RuntimeException {
    public KitchenOrderOperationException(String message) {
        super(message);
    }

    public KitchenOrderOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}