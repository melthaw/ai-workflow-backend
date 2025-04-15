package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.service.WorkflowMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of WorkflowMonitorService using logging
 * In a production environment, this would likely be replaced with
 * a more robust implementation that uses a database for persistence.
 */
@Slf4j
@Service
public class WorkflowMonitorServiceImpl implements WorkflowMonitorService {
    
    // 简单内存缓存，用于存储正在进行的工作流执行
    // 注意：在生产环境中应使用外部存储（如数据库）
    private final ConcurrentHashMap<String, Map<String, Object>> activeExecutions = new ConcurrentHashMap<>();
    
    @Override
    public String recordWorkflowStart(String workflowId, Map<String, Object> inputs) {
        String executionId = generateExecutionId(workflowId);
        
        log.info("Workflow execution started: {} (ID: {})", workflowId, executionId);
        log.debug("Workflow inputs: {}", sanitizeMap(inputs));
        
        // 存储执行信息
        Map<String, Object> executionInfo = new ConcurrentHashMap<>();
        executionInfo.put("workflowId", workflowId);
        executionInfo.put("startTime", System.currentTimeMillis());
        executionInfo.put("inputs", sanitizeMap(inputs));
        executionInfo.put("nodesProcessed", 0);
        
        activeExecutions.put(executionId, executionInfo);
        
        return executionId;
    }
    
    @Override
    public void recordWorkflowComplete(String executionId, String workflowId, Map<String, Object> outputs, 
            long durationMs, int nodesProcessed) {
        
        log.info("Workflow execution completed: {} (ID: {}) in {}ms, processed {} nodes", 
                workflowId, executionId, durationMs, nodesProcessed);
        log.debug("Workflow outputs: {}", sanitizeMap(outputs));
        
        // 清理执行信息
        activeExecutions.remove(executionId);
    }
    
    @Override
    public void recordWorkflowError(String executionId, String workflowId, String errorMessage, 
            long durationMs, int nodesProcessed) {
        
        log.error("Workflow execution failed: {} (ID: {}) in {}ms after processing {} nodes. Error: {}", 
                workflowId, executionId, durationMs, nodesProcessed, errorMessage);
        
        // 清理执行信息
        activeExecutions.remove(executionId);
    }
    
    @Override
    public void recordNodeStart(String executionId, String workflowId, String nodeId, 
            String nodeName, String nodeType, Map<String, Object> inputs) {
        
        log.debug("Node execution started: {} ({}) [{}] in workflow {} (ID: {})", 
                nodeName, nodeId, nodeType, workflowId, executionId);
        log.trace("Node inputs: {}", sanitizeMap(inputs));
        
        // 更新处理的节点计数
        activeExecutions.computeIfPresent(executionId, (id, info) -> {
            int count = (int) info.getOrDefault("nodesProcessed", 0);
            info.put("nodesProcessed", count + 1);
            return info;
        });
    }
    
    @Override
    public void recordNodeComplete(String executionId, String workflowId, String nodeId, 
            NodeOutDTO result, long durationMs) {
        
        if (result.getStatus() == 0) {
            log.debug("Node execution completed successfully: {} in workflow {} (ID: {}) in {}ms", 
                    nodeId, workflowId, executionId, durationMs);
            log.trace("Node outputs: {}", sanitizeMap(result.getOutputs()));
        } else {
            log.warn("Node execution failed: {} in workflow {} (ID: {}) in {}ms. Error: {}", 
                    nodeId, workflowId, executionId, durationMs, result.getMessage());
        }
    }
    
    /**
     * Generate a unique execution ID
     */
    private String generateExecutionId(String workflowId) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("wf_%s_%s_%s", 
                workflowId.substring(0, Math.min(8, workflowId.length())), 
                timestamp, 
                shortUuid);
    }
    
    /**
     * Sanitize potentially sensitive data in maps
     * In a real environment, this would mask tokens, passwords, etc.
     */
    private Map<String, Object> sanitizeMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        // 简单实现：返回相同的映射
        // 在生产环境中，这将屏蔽敏感信息
        return map;
    }
} 