package com.fastgpt.ai.service;

import java.util.Map;

/**
 * Service for managing variables in workflow executions
 * Separates system variables from user variables and provides scoped access
 */
public interface VariableManager {
    
    /**
     * Get a variable by key
     * @param key Variable key
     * @return Variable value, or null if not found
     */
    Object getVariable(String key);
    
    /**
     * Get a variable by key with type conversion
     * @param key Variable key
     * @param type Type to convert to
     * @return Variable value converted to specified type, or null if not found
     */
    <T> T getVariable(String key, Class<T> type);
    
    /**
     * Check if a variable exists
     * @param key Variable key
     * @return true if variable exists
     */
    boolean hasVariable(String key);
    
    /**
     * Set a user variable
     * @param key Variable key
     * @param value Variable value
     */
    void setUserVariable(String key, Object value);
    
    /**
     * Set a system variable
     * @param key Variable key
     * @param value Variable value
     */
    void setSystemVariable(String key, Object value);
    
    /**
     * Get all user variables
     * @return Map of user variables
     */
    Map<String, Object> getUserVariables();
    
    /**
     * Get all system variables
     * @return Map of system variables
     */
    Map<String, Object> getSystemVariables();
    
    /**
     * Get all variables (user and system)
     * @return Map of all variables
     */
    Map<String, Object> getAllVariables();
    
    /**
     * Remove a variable by key
     * @param key Variable key
     * @return true if variable was removed
     */
    boolean removeVariable(String key);
    
    /**
     * Clear all user variables
     */
    void clearUserVariables();
    
    /**
     * Create a new execution scope, creating a new context with the existing variables
     * @return New variable manager with copied variables
     */
    VariableManager createScope();
    
    /**
     * Format a value according to the expected type
     * @param value Value to format
     * @param targetType Target type name (string, number, boolean, object, etc.)
     * @return Formatted value
     */
    Object formatValue(Object value, String targetType);
} 