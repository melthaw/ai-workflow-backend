package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.HashMap;

/**
 * 节点执行输出DTO
 * 对标Next.js版本中的NodeOutDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeOutDTO {
    /**
     * 输出值映射
     */
    private Map<String, Object> output;
    
    /**
     * 节点执行响应数据
     */
    private Map<String, Object> responseData;
    
    /**
     * 资源使用情况
     */
    private Map<String, Object> usages;
    
    /**
     * 新变量
     */
    private Map<String, Object> newVariables;
    
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
    
    /**
     * 提供兼容性的方法，确保现有代码可以正常工作
     */
    public Map<String, Object> getOutputs() {
        return output != null ? output : new HashMap<>();
    }
    
    /**
     * 提供兼容性的方法，确保现有代码可以正常工作
     */
    public void setOutputs(Map<String, Object> outputs) {
        this.output = outputs;
    }
} 