package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a progress update during workflow streaming
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamProgressUpdate {
    
    /**
     * The execution ID
     */
    private String executionId;
    
    /**
     * The workflow ID
     */
    private String workflowId;
    
    /**
     * The type of update (chunk, node_complete, edge_update, heartbeat, error, complete)
     */
    private UpdateType updateType;
    
    /**
     * Text content for the current stream chunk
     */
    private String content;
    
    /**
     * Current node being executed 
     */
    private String currentNodeId;
    
    /**
     * List of completed node IDs
     */
    private List<String> completedNodeIds;
    
    /**
     * Map of node execution metrics
     */
    private Map<String, NodeExecutionMetrics> nodeMetrics;
    
    /**
     * Error message if any
     */
    private String errorMessage;
    
    /**
     * Flag indicating if this is the final update
     */
    private boolean complete;
    
    /**
     * Type of progress update
     */
    public enum UpdateType {
        CHUNK,           // Text chunk from node output
        NODE_COMPLETE,   // Node execution completed
        EDGE_UPDATE,     // Edge status updated
        HEARTBEAT,       // Heartbeat to keep connection alive
        ERROR,           // Error occurred
        COMPLETE         // Workflow execution completed
    }
} 