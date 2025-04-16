package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for node execution metrics in workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionMetrics {
    
    /**
     * Node ID
     */
    private String nodeId;
    
    /**
     * Node type
     */
    private String nodeType;
    
    /**
     * Total execution time in milliseconds
     */
    private Long executionTimeMs;
    
    /**
     * Start time in milliseconds since epoch
     */
    private Long startTimeMs;
    
    /**
     * End time in milliseconds since epoch
     */
    private Long endTimeMs;
    
    /**
     * Memory usage in MB
     */
    private Long memoryUsageMb;
    
    /**
     * CPU usage percentage (0-100)
     */
    private Double cpuUsagePercent;
    
    /**
     * Number of input tokens (for AI nodes)
     */
    private Integer inputTokens;
    
    /**
     * Number of output tokens (for AI nodes)
     */
    private Integer outputTokens;
    
    /**
     * Total tokens processed (for AI nodes)
     */
    private Integer totalTokens;
    
    /**
     * Tokens processed per second (for AI nodes)
     */
    private Double tokenRate;
    
    /**
     * Execution cost (for paid services)
     */
    private Double cost;
    
    /**
     * Number of retries
     */
    private Integer retryCount;
    
    /**
     * Error message (if execution failed)
     */
    private String errorMessage;
    
    /**
     * Whether the node execution was successful
     */
    private Boolean successful;
    
    /**
     * Additional metrics specific to the node type
     */
    private Object additionalMetrics;
} 