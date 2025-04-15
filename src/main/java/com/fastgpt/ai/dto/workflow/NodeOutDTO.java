package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for node execution output
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeOutDTO {
    
    /**
     * Execution status code (0 = success, others = error)
     */
    private int status;
    
    /**
     * Error message if status is not 0
     */
    private String message;
    
    /**
     * Output values from the node execution
     */
    private Map<String, Object> outputs;
    
    /**
     * Metadata about the execution (timing, used resources, etc.)
     */
    private Map<String, Object> metadata;

    /**
     * Create a success result
     */
    public static NodeOutDTO success(Map<String, Object> outputs) {
        return NodeOutDTO.builder()
                .status(0)
                .outputs(outputs)
                .build();
    }
    
    /**
     * Create a success result with metadata
     */
    public static NodeOutDTO success(Map<String, Object> outputs, Map<String, Object> metadata) {
        return NodeOutDTO.builder()
                .status(0)
                .outputs(outputs)
                .metadata(metadata)
                .build();
    }
    
    /**
     * Create an error result
     */
    public static NodeOutDTO error(String message) {
        return NodeOutDTO.builder()
                .status(1)
                .message(message)
                .build();
    }
    
    /**
     * Create an error result with status code
     */
    public static NodeOutDTO error(int status, String message) {
        return NodeOutDTO.builder()
                .status(status)
                .message(message)
                .build();
    }
} 