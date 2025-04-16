package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;

import java.util.Map;

/**
 * Interface for node dispatchers that execute specific node types
 */
public interface NodeDispatcher {
    
    /**
     * Get the node type this dispatcher can handle
     * @return The node type string
     */
    String getNodeType();
    
    /**
     * Dispatch and execute a node with the given inputs
     * @param node The node to execute
     * @param inputs The input values for the node
     * @return The execution result
     */
    NodeOutDTO dispatch(Node node, Map<String, Object> inputs);
    
    /**
     * Check if this dispatcher can handle the given node
     * 
     * @param node The node to check
     * @return true if this dispatcher can handle the node, false otherwise
     */
    default boolean canHandle(Node node) {
        return node != null && getNodeType().equals(node.getType());
    }
} 