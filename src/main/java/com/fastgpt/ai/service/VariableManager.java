package com.fastgpt.ai.service;

import com.fastgpt.ai.entity.workflow.Node;

import java.util.List;
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
    
    /**
     * 替换文本中的变量引用
     * 支持替换：
     * 1. 系统变量: {{变量名}}
     * 2. 节点输出引用: {{$nodeId.outputKey$}}
     * 
     * @param text 包含变量引用的文本
     * @param nodes 运行时节点列表
     * @param variables 当前变量映射
     * @return 替换后的值
     */
    Object replaceVariables(Object value, List<Node> nodes, Map<String, Object> variables);
    
    /**
     * 获取引用变量的实际值
     * 
     * @param value 可能包含变量引用的值
     * @param nodes 运行时节点列表
     * @param variables 当前变量映射
     * @return 解析后的值
     */
    Object getReferenceVariableValue(Object value, List<Node> nodes, Map<String, Object> variables);
    
    /**
     * 移除系统变量
     * 
     * @param variables 包含系统变量的变量映射
     * @param externalVariables 外部变量
     * @return 移除系统变量后的变量映射
     */
    Map<String, Object> removeSystemVariables(Map<String, Object> variables, Map<String, Object> externalVariables);
} 