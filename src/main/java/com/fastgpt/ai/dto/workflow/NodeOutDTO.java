package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.HashMap;

/**
 * DTO for node execution output
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeOutDTO {
    
    /**
     * Node ID
     */
    private String nodeId;
    
    /**
     * Whether the node execution was successful
     */
    private boolean success;
    
    /**
     * Error message if execution failed
     */
    private String error;
    
    /**
     * Output values from the node
     */
    private Map<String, Object> outputs;
    
    /**
     * Metadata about the node execution
     */
    private Map<String, Object> metadata;
    
    /**
     * Whether the node execution has been suspended for user interaction
     */
    private boolean suspended;
    
    /**
     * ID of the interaction (for suspended nodes waiting for user interaction)
     */
    private String interactionId;
    
    /**
     * Create a successful result
     * @param outputs Node outputs
     * @return A success result
     */
    public static NodeOutDTO success(Map<String, Object> outputs) {
        return NodeOutDTO.builder()
                .success(true)
                .outputs(outputs != null ? outputs : new HashMap<>())
                .build();
    }
    
    /**
     * Create a successful result with metadata
     * @param outputs Node outputs
     * @param metadata Node metadata
     * @return A success result
     */
    public static NodeOutDTO success(Map<String, Object> outputs, Map<String, Object> metadata) {
        return NodeOutDTO.builder()
                .success(true)
                .outputs(outputs != null ? outputs : new HashMap<>())
                .metadata(metadata != null ? metadata : new HashMap<>())
                .build();
    }
    
    /**
     * Create an error result
     * @param errorMessage Error message
     * @return An error result
     */
    public static NodeOutDTO error(String errorMessage) {
        return NodeOutDTO.builder()
                .success(false)
                .error(errorMessage)
                .outputs(new HashMap<>())
                .build();
    }
    
    /**
     * Create a suspended result (waiting for user interaction)
     * @param outputs Node outputs
     * @return A suspended result
     */
    public static NodeOutDTO suspended(Map<String, Object> outputs) {
        return NodeOutDTO.builder()
                .success(true)
                .suspended(true)
                .outputs(outputs != null ? outputs : new HashMap<>())
                .build();
    }
    
    /**
     * Create a suspended result with metadata
     * @param outputs Node outputs
     * @param metadata Node metadata
     * @return A suspended result
     */
    public static NodeOutDTO suspended(Map<String, Object> outputs, Map<String, Object> metadata) {
        return NodeOutDTO.builder()
                .success(true)
                .suspended(true)
                .outputs(outputs != null ? outputs : new HashMap<>())
                .metadata(metadata != null ? metadata : new HashMap<>())
                .build();
    }
    
    /**
     * Create a suspended result with interaction ID
     * @param outputs Node outputs
     * @param interactionId ID of the interaction
     * @return A suspended result
     */
    public static NodeOutDTO suspended(Map<String, Object> outputs, String interactionId) {
        return NodeOutDTO.builder()
                .success(true)
                .suspended(true)
                .interactionId(interactionId)
                .outputs(outputs != null ? outputs : new HashMap<>())
                .build();
    }
    
    /**
     * Create a suspended result with interaction ID and metadata
     * @param outputs Node outputs
     * @param interactionId ID of the interaction
     * @param metadata Node metadata
     * @return A suspended result
     */
    public static NodeOutDTO suspended(Map<String, Object> outputs, String interactionId, Map<String, Object> metadata) {
        return NodeOutDTO.builder()
                .success(true)
                .suspended(true)
                .interactionId(interactionId)
                .outputs(outputs != null ? outputs : new HashMap<>())
                .metadata(metadata != null ? metadata : new HashMap<>())
                .build();
    }
} 