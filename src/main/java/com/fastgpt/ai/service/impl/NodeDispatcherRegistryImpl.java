package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import com.fastgpt.ai.service.NodeDispatcherRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点调度器注册表实现类
 */
@Slf4j
@Service
public class NodeDispatcherRegistryImpl implements NodeDispatcherRegistry {
    
    private final Map<String, NodeDispatcher> dispatchers = new ConcurrentHashMap<>();
    private final List<NodeDispatcher> dispatcherList;
    
    /**
     * 构造函数，注入所有实现了NodeDispatcher的组件
     */
    public NodeDispatcherRegistryImpl(List<NodeDispatcher> dispatcherList) {
        this.dispatcherList = dispatcherList;
    }
    
    /**
     * 初始化时注册所有调度器
     */
    @PostConstruct
    public void init() {
        for (NodeDispatcher dispatcher : dispatcherList) {
            registerDispatcher(dispatcher.getNodeType(), dispatcher);
            log.info("Registered node dispatcher for type: {}", dispatcher.getNodeType());
        }
        log.info("Node dispatcher registry initialized with {} dispatchers", dispatchers.size());
    }
    
    @Override
    public void registerDispatcher(String nodeType, NodeDispatcher dispatcher) {
        dispatchers.put(nodeType, dispatcher);
    }
    
    @Override
    public NodeDispatcher getDispatcher(String nodeType) {
        return dispatchers.get(nodeType);
    }
    
    @Override
    public boolean hasDispatcher(String nodeType) {
        return dispatchers.containsKey(nodeType);
    }
    
    @Override
    public NodeOutDTO dispatchNode(Node node, Map<String, Object> inputs) {
        String nodeType = node.getType().toString();
        NodeDispatcher dispatcher = getDispatcher(nodeType);
        if (dispatcher == null) {
            throw new IllegalArgumentException("No dispatcher found for node type: " + nodeType);
        }
        return dispatcher.dispatch(node, inputs);
    }
    
    @Override
    public Map<String, NodeDispatcher> getAllDispatchers() {
        return new HashMap<>(dispatchers);
    }
} 