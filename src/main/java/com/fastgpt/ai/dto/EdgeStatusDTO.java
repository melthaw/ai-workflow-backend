package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for edge execution status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeStatusDTO {
    
    /**
     * ID of the edge
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
     * Execution status of the edge (waiting, active, skipped)
     */
    private EdgeStatusEnum status;
    
    /**
     * Timestamp when the edge was activated (if status is active)
     */
    private Long activatedAt;
    
    /**
     * Enum for edge execution status
     */
    public enum EdgeStatusEnum {
        WAITING,
        ACTIVE,
        SKIPPED
    }
} 