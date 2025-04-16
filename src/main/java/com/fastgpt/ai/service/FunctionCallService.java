package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.FunctionCallResult;
import com.fastgpt.ai.dto.FunctionDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing function registrations and executions
 */
public interface FunctionCallService {
    
    /**
     * Register a function
     * @param definition Function definition
     * @return The registered function definition
     */
    FunctionDefinition registerFunction(FunctionDefinition definition);
    
    /**
     * Register multiple functions
     * @param definitions List of function definitions
     * @return The list of registered function definitions
     */
    List<FunctionDefinition> registerFunctions(List<FunctionDefinition> definitions);
    
    /**
     * Get a function by ID
     * @param functionId Function ID
     * @return Optional containing the function definition if found
     */
    Optional<FunctionDefinition> getFunction(String functionId);
    
    /**
     * Get a function by name
     * @param name Function name
     * @return Optional containing the function definition if found
     */
    Optional<FunctionDefinition> getFunctionByName(String name);
    
    /**
     * Get all registered functions
     * @return List of all function definitions
     */
    List<FunctionDefinition> getAllFunctions();
    
    /**
     * Get functions by category
     * @param category Category name
     * @return List of matching function definitions
     */
    List<FunctionDefinition> getFunctionsByCategory(String category);
    
    /**
     * Call a function by ID
     * @param functionId Function ID
     * @param arguments Function arguments
     * @return Function call result
     */
    FunctionCallResult callFunction(String functionId, Map<String, Object> arguments);
    
    /**
     * Call a function by name
     * @param name Function name
     * @param arguments Function arguments
     * @return Function call result
     */
    FunctionCallResult callFunctionByName(String name, Map<String, Object> arguments);
    
    /**
     * Validate function arguments against the function schema
     * @param functionId Function ID
     * @param arguments Arguments to validate
     * @return True if the arguments are valid, false otherwise
     */
    boolean validateArguments(String functionId, Map<String, Object> arguments);
    
    /**
     * Unregister a function
     * @param functionId Function ID
     * @return True if the function was unregistered, false if it didn't exist
     */
    boolean unregisterFunction(String functionId);
} 