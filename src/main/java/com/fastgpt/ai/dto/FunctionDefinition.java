package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a function that can be called by AI models
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDefinition {
    
    /**
     * Unique identifier for the function
     */
    private String id;
    
    /**
     * Name of the function
     */
    private String name;
    
    /**
     * Description of what the function does
     */
    private String description;
    
    /**
     * Parameters for the function in JSON Schema format
     */
    private Map<String, Object> parameters;
    
    /**
     * Return type of the function
     */
    private String returnType;
    
    /**
     * Whether the function is required to be called
     */
    private Boolean required;
    
    /**
     * List of sample invocations for documentation
     */
    private List<Map<String, Object>> examples;
    
    /**
     * Optional type information for the parameters
     */
    private Map<String, String> parameterTypes;
    
    /**
     * Category of the function (e.g., "data", "utility", "math")
     */
    private String category;
    
    /**
     * Whether the function should be executed in a sandbox
     */
    private Boolean sandbox;
    
    /**
     * Maximum time in milliseconds the function is allowed to run
     */
    private Long timeoutMs;
    
    /**
     * Optional schema for the function's return value
     */
    private Map<String, Object> returnSchema;
} 