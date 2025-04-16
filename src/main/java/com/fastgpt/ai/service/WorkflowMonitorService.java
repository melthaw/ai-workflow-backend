package com.fastgpt.ai.service;

import java.util.Map;

/**
 * Service for monitoring and tracking workflow executions
 */
public interface WorkflowMonitorService {
    
    /**
     * Start tracking a new workflow execution
     * 
     * @param workflowId The workflow ID
     * @param inputs The workflow inputs
     * @return The execution ID
     */
    String startExecution(String workflowId, Map<String, Object> inputs);
    
    /**
     * Start tracking a new workflow execution with specific execution ID
     * 
     * @param workflowId The workflow ID
     * @param inputs The workflow inputs
     * @param executionId The execution ID
     * @return The execution ID (same as input executionId)
     */
    String startExecution(String workflowId, Map<String, Object> inputs, String executionId);
    
    /**
     * Record successful completion of a workflow execution
     * 
     * @param executionId The execution ID
     * @param outputs The workflow outputs
     */
    void completeExecution(String executionId, Map<String, Object> outputs);
    
    /**
     * Record failure of a workflow execution
     * 
     * @param executionId The execution ID
     * @param error The error message
     */
    void failExecution(String executionId, String error);
    
    /**
     * Record start of a node execution
     * 
     * @param executionId The workflow execution ID
     * @param nodeId The node ID
     * @param nodeType The type of node
     * @param inputs The node inputs
     * @return The node execution ID
     */
    String recordNodeStart(String executionId, String nodeId, String nodeType, Map<String, Object> inputs);
    
    /**
     * Record successful completion of a node
     * 
     * @param nodeExecutionId The node execution ID
     * @param outputs The node outputs
     * @param metadata Execution metadata
     */
    void recordNodeComplete(String nodeExecutionId, Map<String, Object> outputs, Map<String, Object> metadata);
    
    /**
     * Record a node execution error
     * 
     * @param nodeExecutionId The node execution ID
     * @param error The error message
     * @param metadata Execution metadata
     */
    void recordNodeError(String nodeExecutionId, String error, Map<String, Object> metadata);
    
    /**
     * Get execution data for a workflow execution
     * 
     * @param executionId The execution ID
     * @return Map containing execution data
     */
    Map<String, Object> getExecutionData(String executionId);
    
    /**
     * Get the final result of a workflow execution
     * 
     * @param executionId The execution ID
     * @return Map containing execution results
     */
    Map<String, Object> getExecutionResult(String executionId);
    
    /**
     * Get workflow execution metadata
     * 
     * @param workflowId The workflow ID
     * @return Map containing execution metadata
     */
    Map<String, Object> getWorkflowExecutionMetadata(String workflowId);
    
    /**
     * Check if an execution is complete (either success or failure)
     * 
     * @param executionId The execution ID
     * @return true if execution is complete, false otherwise
     */
    boolean isExecutionComplete(String executionId);
} 