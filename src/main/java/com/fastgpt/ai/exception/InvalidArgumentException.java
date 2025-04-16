package com.fastgpt.ai.exception;

/**
 * Exception thrown when an invalid argument is provided
 */
public class InvalidArgumentException extends RuntimeException {
    
    public InvalidArgumentException(String message) {
        super(message);
    }
    
    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
} 