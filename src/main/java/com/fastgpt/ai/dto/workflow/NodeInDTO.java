package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for node execution input
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeInDTO {
    
    /**
     * Node identifier
     */
    private String nodeId;
    
    /**
     * Input values for the node execution
     */
    private Map<String, Object> inputs;
    
    /**
     * Additional metadata for execution
     */
    private Map<String, Object> metadata;

    /**
     * Create a basic input DTO
     */
    public static NodeInDTO create(String nodeId, Map<String, Object> inputs) {
        return NodeInDTO.builder()
                .nodeId(nodeId)
                .inputs(inputs)
                .build();
    }
    
    /**
     * Create an input DTO with metadata
     */
    public static NodeInDTO create(String nodeId, Map<String, Object> inputs, Map<String, Object> metadata) {
        return NodeInDTO.builder()
                .nodeId(nodeId)
                .inputs(inputs)
                .metadata(metadata)
                .build();
    }
} 