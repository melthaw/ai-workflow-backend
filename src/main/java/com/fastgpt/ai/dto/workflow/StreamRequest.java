package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for streaming workflow execution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamRequest {
    
    /**
     * Input values for workflow execution
     */
    private Map<String, Object> inputs;
    
    /**
     * Optional execution configuration
     */
    private Map<String, Object> config;
} 