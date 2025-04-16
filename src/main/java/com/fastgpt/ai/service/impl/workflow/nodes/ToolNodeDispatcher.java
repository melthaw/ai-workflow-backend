package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.NodeDispatcher;
import com.fastgpt.ai.service.impl.workflow.tool.ToolRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Node dispatcher for calling system tools
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolNodeDispatcher implements NodeDispatcher {

    private final ToolRegistryService toolRegistry;
    
    private static final String NODE_TYPE = "ai.tool.executor";
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return executeTool(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in tool node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Tool execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Execute tool based on inputs
     */
    private NodeOutDTO executeTool(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing tool node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get tool name from config or input
            String toolName = getStringParam(nodeData, "toolName", "");
            if (toolName.isEmpty() && inputs.containsKey("toolName")) {
                toolName = getStringParam(inputs, "toolName", "");
            }
            
            if (toolName.isEmpty()) {
                return NodeOutDTO.error("No tool name provided");
            }
            
            // Check if tool exists
            if (!toolRegistry.hasTool(toolName)) {
                return NodeOutDTO.error("Tool not found: " + toolName);
            }
            
            // Get parameters from inputs
            Map<String, Object> parameters = new HashMap<>();
            
            // First add parameters from node config (default values)
            if (nodeData.containsKey("parameters") && nodeData.get("parameters") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configParams = (Map<String, Object>) nodeData.get("parameters");
                parameters.putAll(configParams);
            }
            
            // Then override with parameters from inputs
            if (inputs.containsKey("parameters") && inputs.get("parameters") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> inputParams = (Map<String, Object>) inputs.get("parameters");
                parameters.putAll(inputParams);
            }
            
            // Execute tool
            log.info("Executing tool: {} with parameters: {}", toolName, parameters);
            Object result;
            
            try {
                result = toolRegistry.executeTool(toolName, parameters);
            } catch (Exception e) {
                log.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
                return NodeOutDTO.error("Tool execution failed: " + e.getMessage());
            }
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", result);
            outputs.put("toolName", toolName);
            outputs.put("parameters", parameters);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("toolName", toolName);
            
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error in tool execution: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to convert Node to NodeDefDTO
     */
    private NodeDefDTO convertToNodeDefDTO(Node node) {
        NodeDefDTO nodeDefDTO = new NodeDefDTO();
        nodeDefDTO.setId(node.getId());
        nodeDefDTO.setType(node.getType());
        nodeDefDTO.setData(node.getData());
        return nodeDefDTO;
    }
    
    /**
     * Helper method to get a string parameter with default value
     */
    private String getStringParam(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
} 