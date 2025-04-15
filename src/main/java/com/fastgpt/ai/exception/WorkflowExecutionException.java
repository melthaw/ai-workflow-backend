package com.fastgpt.ai.exception;

/**
 * Exception thrown when workflow execution fails
 */
public class WorkflowExecutionException extends RuntimeException {
    
    public WorkflowExecutionException(String message) {
        super(message);
    }
    
    public WorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
} 