package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents the result of a function call
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCallResult {
    
    /**
     * ID of the function that was called
     */
    private String functionId;
    
    /**
     * Name of the function that was called
     */
    private String name;
    
    /**
     * Arguments passed to the function
     */
    private Map<String, Object> arguments;
    
    /**
     * Result of the function call
     */
    private Object result;
    
    /**
     * Whether the function call was successful
     */
    private boolean success;
    
    /**
     * Error message if the function call failed
     */
    private String error;
    
    /**
     * Time the function was called
     */
    private LocalDateTime calledAt;
    
    /**
     * Time the function call completed
     */
    private LocalDateTime completedAt;
    
    /**
     * Execution time in milliseconds
     */
    private long executionTimeMs;
    
    /**
     * Additional metadata about the function call
     */
    private Map<String, Object> metadata;
    
    /**
     * Create a success result
     */
    public static FunctionCallResult success(String functionId, String name, Map<String, Object> arguments, Object result, long executionTimeMs) {
        return FunctionCallResult.builder()
                .functionId(functionId)
                .name(name)
                .arguments(arguments)
                .result(result)
                .success(true)
                .calledAt(LocalDateTime.now().minusNanos(executionTimeMs * 1_000_000))
                .completedAt(LocalDateTime.now())
                .executionTimeMs(executionTimeMs)
                .build();
    }
    
    /**
     * Create an error result
     */
    public static FunctionCallResult error(String functionId, String name, Map<String, Object> arguments, String error, long executionTimeMs) {
        return FunctionCallResult.builder()
                .functionId(functionId)
                .name(name)
                .arguments(arguments)
                .success(false)
                .error(error)
                .calledAt(LocalDateTime.now().minusNanos(executionTimeMs * 1_000_000))
                .completedAt(LocalDateTime.now())
                .executionTimeMs(executionTimeMs)
                .build();
    }
} 