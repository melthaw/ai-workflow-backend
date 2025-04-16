package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;

import java.util.Map;

/**
 * Registry for node dispatchers
 */
public interface NodeDispatcherRegistry {
    
    /**
     * Register a dispatcher for a node type
     * @param nodeType The node type
     * @param dispatcher The dispatcher to register
     */
    void registerDispatcher(String nodeType, NodeDispatcher dispatcher);
    
    /**
     * Get a dispatcher for a node type
     * @param nodeType The node type
     * @return The dispatcher
     * @throws IllegalArgumentException if no dispatcher is found
     */
    NodeDispatcher getDispatcher(String nodeType);
    
    /**
     * Check if a dispatcher exists for a node type
     * @param nodeType The node type
     * @return True if a dispatcher exists, false otherwise
     */
    boolean hasDispatcher(String nodeType);
    
    /**
     * Dispatch a node execution
     * @param node The node to execute
     * @param inputs The inputs for node execution
     * @return The result of node execution
     */
    NodeOutDTO dispatchNode(Node node, Map<String, Object> inputs);
    
    /**
     * Get all registered dispatchers
     * @return Map of node type to dispatcher
     */
    Map<String, NodeDispatcher> getAllDispatchers();
}