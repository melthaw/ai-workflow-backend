package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for workflow edge connections
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDTO {
    
    /**
     * Unique ID of the edge
     */
    private String id;
    
    /**
     * ID of the source node
     */
    private String sourceNodeId;
    
    /**
     * ID of the target node
     */
    private String targetNodeId;
    
    /**
     * Key of the output from the source node
     */
    private String sourceOutputKey;
    
    /**
     * Key of the input to the target node
     */
    private String targetInputKey;
    
    /**
     * Optional label for the connection
     */
    private String label;
    
    /**
     * Optional style configuration
     */
    private Object style;
} 