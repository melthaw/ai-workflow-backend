package com.fastgpt.ai.exception;

/**
 * Exception thrown when there is an error executing a function
 */
public class FunctionExecutionException extends RuntimeException {
    
    public FunctionExecutionException(String message) {
        super(message);
    }
    
    public FunctionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
} 