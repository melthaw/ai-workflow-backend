package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for workflow node connections
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDTO {
    
    /**
     * ID of the source node
     */
    private String sourceNodeId;
    
    /**
     * Output field name from the source node
     */
    private String sourceHandle;
    
    /**
     * ID of the target node
     */
    private String targetNodeId;
    
    /**
     * Input field name to the target node
     */
    private String targetHandle;
} 