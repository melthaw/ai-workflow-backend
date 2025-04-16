package com.fastgpt.ai.service.function;

import com.fastgpt.ai.dto.function.FunctionDefinition;

import java.util.List;
import java.util.Map;

/**
 * Interface for function providers that offer reusable functions
 */
public interface FunctionProvider {
    
    /**
     * Get list of functions provided by this provider
     * @return List of function definitions
     */
    List<FunctionDefinition> getFunctions();
    
    /**
     * Execute a function by name with given parameters
     * @param name Function name
     * @param parameters Function parameters
     * @return Result of function execution
     * @throws IllegalArgumentException if function is not supported or parameters are invalid
     */
    Map<String, Object> executeFunction(String name, Map<String, Object> parameters);
    
    /**
     * Check if this provider offers core system functions
     * @return true if provides core functions, false otherwise
     */
    boolean isCore();
} 