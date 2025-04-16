package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import com.fastgpt.ai.service.NodeDispatcherRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for node dispatchers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeDispatcherRegistryImpl implements NodeDispatcherRegistry {
    
    // List of all node dispatchers (injected by Spring)
    private final List<NodeDispatcher> dispatchers;
    
    // Map of node type to dispatcher
    private final Map<String, NodeDispatcher> dispatcherMap = new HashMap<>();
    
    /**
     * Initialize the registry by mapping dispatchers to node types
     */
    @PostConstruct
    public void init() {
        log.info("Initializing node dispatcher registry");
        
        for (NodeDispatcher dispatcher : dispatchers) {
            String nodeType = dispatcher.getNodeType();
            dispatcherMap.put(nodeType, dispatcher);
            log.info("Registered dispatcher for node type: {}", nodeType);
        }
        
        log.info("Node dispatcher registry initialized with {} dispatchers", dispatcherMap.size());
    }
    
    @Override
    public void registerDispatcher(String nodeType, NodeDispatcher dispatcher) {
        dispatcherMap.put(nodeType, dispatcher);
        log.info("Manually registered dispatcher for node type: {}", nodeType);
    }
    
    @Override
    public NodeDispatcher getDispatcher(String nodeType) {
        NodeDispatcher dispatcher = dispatcherMap.get(nodeType);
        
        if (dispatcher == null) {
            throw new IllegalArgumentException("No dispatcher found for node type: " + nodeType);
        }
        
        return dispatcher;
    }
    
    @Override
    public boolean hasDispatcher(String nodeType) {
        return dispatcherMap.containsKey(nodeType);
    }
    
    @Override
    public NodeOutDTO dispatchNode(Node node, Map<String, Object> inputs) {
        if (node == null) {
            return NodeOutDTO.builder()
                    .nodeId(null)
                    .success(false)
                    .error("Node is null")
                    .build();
        }
        
        String nodeType = node.getType();
        
        if (nodeType == null || nodeType.isEmpty()) {
            return NodeOutDTO.builder()
                    .nodeId(node.getId())
                    .success(false)
                    .error("Node type is missing")
                    .build();
        }
        
        try {
            NodeDispatcher dispatcher = getDispatcher(nodeType);
            
            // Sanitize inputs
            Map<String, Object> sanitizedInputs = inputs != null ? new HashMap<>(inputs) : new HashMap<>();
            
            // Execute node
            long startTime = System.currentTimeMillis();
            NodeOutDTO result = dispatcher.dispatch(node, sanitizedInputs);
            long endTime = System.currentTimeMillis();
            
            // Add execution metrics if not already present
            if (result.getMetadata() == null) {
                result.setMetadata(new HashMap<>());
            }
            
            result.getMetadata().putIfAbsent("executionTimeMs", endTime - startTime);
            result.getMetadata().putIfAbsent("nodeType", nodeType);
            
            // Ensure node ID is set
            if (result.getNodeId() == null) {
                result.setNodeId(node.getId());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error dispatching node: {}", node.getId(), e);
            return NodeOutDTO.builder()
                    .nodeId(node.getId())
                    .success(false)
                    .error("Execution error: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public Map<String, NodeDispatcher> getAllDispatchers() {
        return new HashMap<>(dispatcherMap);
    }
} 