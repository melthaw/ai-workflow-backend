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
     * Type of the node (e.g., "ai", "function", "input", "output")
     */
    private String type;
    
    /**
     * Name/label of the node
     */
    private String name;
    
    /**
     * X position in the workflow canvas
     */
    private Integer x;
    
    /**
     * Y position in the workflow canvas
     */
    private Integer y;
    
    /**
     * Input connections to this node
     */
    private List<ConnectionDTO> inputs;
    
    /**
     * Output connections from this node
     */
    private List<ConnectionDTO> outputs;
    
    /**
     * Node-specific configuration data
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
} 