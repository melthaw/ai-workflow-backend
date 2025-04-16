package com.fastgpt.ai.dto;

import com.fastgpt.ai.dto.workflow.EdgeStatusDTO;
import com.fastgpt.ai.dto.workflow.NodeExecutionMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for workflow debug information response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDebugResponse {
    
    /**
     * Workflow ID
     */
    private String workflowId;
    
    /**
     * Execution ID
     */
    private String executionId;
    
    /**
     * List of finished node IDs
     */
    private List<String> finishedNodes;
    
    /**
     * List of nodes to run in the next step
     */
    private List<String> nextStepRunNodes;
    
    /**
     * Total execution time in milliseconds
     */
    private Long totalExecutionTimeMs;
    
    /**
     * Number of nodes processed
     */
    private Integer processedNodesCount;
    
    /**
     * Current context snapshot
     */
    private Map<String, Object> contextSnapshot;
    
    /**
     * Execution errors
     */
    private List<String> errors;
    
    /**
     * Status of all workflow edges
     */
    private List<EdgeStatusDTO> finishedEdges;
    
    /**
     * Execution metrics for each node
     */
    private Map<String, NodeExecutionMetrics> nodeMetrics;
    
    /**
     * Current execution state
     */
    private ExecutionStateEnum executionState;
    
    /**
     * Workflow execution state enum
     */
    public enum ExecutionStateEnum {
        /**
         * Workflow is currently running
         */
        RUNNING,
        
        /**
         * Workflow has completed successfully
         */
        COMPLETED,
        
        /**
         * Workflow execution failed
         */
        FAILED,
        
        /**
         * Workflow is waiting for user interaction
         */
        WAITING_FOR_INTERACTION,
        
        /**
         * Workflow execution was canceled
         */
        CANCELED,
        
        /**
         * Workflow is paused
         */
        PAUSED
    }
} 