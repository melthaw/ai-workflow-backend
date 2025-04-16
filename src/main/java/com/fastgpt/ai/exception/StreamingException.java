package com.fastgpt.ai.exception;

/**
 * Exception thrown during streaming operations
 */
public class StreamingException extends RuntimeException {

    /**
     * Constructs a new streaming exception with the specified detail message.
     * @param message the detail message
     */
    public StreamingException(String message) {
        super(message);
    }

    /**
     * Constructs a new streaming exception with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public StreamingException(String message, Throwable cause) {
        super(message, cause);
    }
} 