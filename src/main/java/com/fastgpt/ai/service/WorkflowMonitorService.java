package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;

import java.util.Map;

/**
 * Service for monitoring workflow execution
 */
public interface WorkflowMonitorService {
    
    /**
     * Record the start of a workflow execution
     * @param workflowId Workflow ID
     * @param inputs Input parameters (sensitive data should be masked)
     * @return Execution ID for correlation
     */
    String recordWorkflowStart(String workflowId, Map<String, Object> inputs);
    
    /**
     * Record the completion of a workflow execution
     * @param executionId Execution ID from recordWorkflowStart
     * @param workflowId Workflow ID
     * @param outputs Output values (sensitive data should be masked)
     * @param durationMs Execution duration in milliseconds
     * @param nodesProcessed Number of nodes processed
     */
    void recordWorkflowComplete(String executionId, String workflowId, Map<String, Object> outputs, 
            long durationMs, int nodesProcessed);
    
    /**
     * Record an error during workflow execution
     * @param executionId Execution ID from recordWorkflowStart
     * @param workflowId Workflow ID
     * @param errorMessage Error message
     * @param durationMs Execution duration in milliseconds until error
     * @param nodesProcessed Number of nodes processed before error
     */
    void recordWorkflowError(String executionId, String workflowId, String errorMessage, 
            long durationMs, int nodesProcessed);
    
    /**
     * Record a node execution start
     * @param executionId Parent workflow execution ID
     * @param workflowId Workflow ID
     * @param nodeId Node ID
     * @param nodeName Node name
     * @param nodeType Node type
     * @param inputs Input values to the node
     */
    void recordNodeStart(String executionId, String workflowId, String nodeId, 
            String nodeName, String nodeType, Map<String, Object> inputs);
    
    /**
     * Record a node execution completion
     * @param executionId Parent workflow execution ID
     * @param workflowId Workflow ID
     * @param nodeId Node ID
     * @param result Node execution result
     * @param durationMs Node execution duration in milliseconds
     */
    void recordNodeComplete(String executionId, String workflowId, String nodeId, 
            NodeOutDTO result, long durationMs);
} 