package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 边状态DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdgeStatusDTO {
    /**
     * 边ID
     */
    private String edgeId;
    
    /**
     * 状态：waiting, active, skipped
     */
    private String status;
} 