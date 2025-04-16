package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for workflow node definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeDefDTO {
    
    /**
     * Unique ID of the node
     */
    private String id;
    
    /**
     * Type of the node (e.g., "chatNode", "datasetSearchNode", "answerNode")
     */
    private String type;
    
    /**
     * Position of the node in the workflow canvas
     */
    private Position position;
    
    /**
     * Node properties (configuration)
     */
    private Map<String, Object> properties;
    
    /**
     * Input connections to this node
     */
    private List<NodeIODTO> inputs;
    
    /**
     * Output connections from this node
     */
    private List<NodeIODTO> outputs;
    
    /**
     * Node-specific configuration data (legacy)
     */
    private Map<String, Object> data;
    
    /**
     * Whether this node is an entry point for the workflow
     */
    private Boolean isEntry;
    
    /**
     * Whether this node is a required node in the workflow
     */
    private Boolean isRequired;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private Integer x;
        private Integer y;
    }
} 