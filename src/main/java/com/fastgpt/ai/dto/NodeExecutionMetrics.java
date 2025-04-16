package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metrics for node execution
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
     * Execution time in milliseconds
     */
    private long executionTimeMs;
    
    /**
     * Input tokens count (for AI nodes)
     */
    private Integer inputTokens;
    
    /**
     * Output tokens count (for AI nodes)
     */
    private Integer outputTokens;
    
    /**
     * Total tokens used (for AI nodes)
     */
    private Integer totalTokens;
    
    /**
     * Cost in USD (for nodes that incur costs)
     */
    private Double cost;
    
    /**
     * Completion tokens per second (for AI nodes)
     */
    private Double tokenRate;
    
    /**
     * Maximum memory used (in MB, for resource-intensive nodes)
     */
    private Long memoryUsageMb;
    
    /**
     * Error message if execution failed
     */
    private String errorMessage;
    
    /**
     * Number of retries before successful execution
     */
    private Integer retryCount;
} 