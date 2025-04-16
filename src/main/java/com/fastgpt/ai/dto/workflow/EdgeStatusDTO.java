package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for edge execution status in workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeStatusDTO {
    
    /**
     * Edge ID
     */
    private String id;
    
    /**
     * Source node ID
     */
    private String sourceNodeId;
    
    /**
     * Target node ID
     */
    private String targetNodeId;
    
    /**
     * Condition expression (if any)
     */
    private String condition;
    
    /**
     * Current status of the edge
     */
    private EdgeStatusEnum status;
    
    /**
     * Time when the edge was activated (in milliseconds since epoch)
     */
    private Long activatedAt;
    
    /**
     * Time when the edge execution was completed (in milliseconds since epoch)
     */
    private Long completedAt;
    
    /**
     * Edge execution status enum
     */
    public enum EdgeStatusEnum {
        /**
         * Edge is waiting to be evaluated
         */
        WAITING,
        
        /**
         * Edge is currently active (being traversed)
         */
        ACTIVE,
        
        /**
         * Edge has been traversed and completed
         */
        COMPLETED,
        
        /**
         * Edge was skipped due to condition evaluation
         */
        SKIPPED,
        
        /**
         * Edge execution failed
         */
        FAILED
    }
} 