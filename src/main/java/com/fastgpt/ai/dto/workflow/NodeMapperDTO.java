package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for mapping between node inputs and outputs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeMapperDTO {
    
    /**
     * Source node identifier
     */
    private String sourceNodeId;
    
    /**
     * Target node identifier 
     */
    private String targetNodeId;
    
    /**
     * Mapping configuration between node outputs and inputs
     * Key: target input field
     * Value: source output field
     */
    private Map<String, String> mapping;
    
    /**
     * Static values to be applied to the target node inputs
     * These values override any mapped values
     */
    private Map<String, Object> staticValues;
    
    /**
     * Create a basic mapper with only field mappings
     */
    public static NodeMapperDTO create(String sourceNodeId, String targetNodeId, Map<String, String> mapping) {
        return NodeMapperDTO.builder()
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .mapping(mapping)
                .build();
    }
    
    /**
     * Create a mapper with field mappings and static values
     */
    public static NodeMapperDTO create(String sourceNodeId, String targetNodeId, 
                                      Map<String, String> mapping, 
                                      Map<String, Object> staticValues) {
        return NodeMapperDTO.builder()
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .mapping(mapping)
                .staticValues(staticValues)
                .build();
    }
} 