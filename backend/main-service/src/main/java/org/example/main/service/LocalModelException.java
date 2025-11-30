package org.example.main.service;

/**
 * Runtime exception used to signal problems starting or talking to the local model.
 */
public class LocalModelException extends RuntimeException {
    public LocalModelException(String message) {
        super(message);
    }

    public LocalModelException(String message, Throwable cause) {
        super(message, cause);
    }
}