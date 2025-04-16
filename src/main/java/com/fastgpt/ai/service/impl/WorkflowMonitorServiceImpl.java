package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.service.WorkflowMonitorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of workflow monitoring service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowMonitorServiceImpl implements WorkflowMonitorService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // In-memory cache for active executions (use Redis in production for distributed support)
    private final Map<String, Map<String, Object>> activeExecutions = new ConcurrentHashMap<>();
    
    // Keys for Redis
    private static final String EXECUTION_KEY_PREFIX = "workflow:execution:";
    private static final String NODE_EXECUTION_KEY_PREFIX = "workflow:node:";
    private static final String WORKFLOW_METRICS_KEY_PREFIX = "workflow:metrics:";
    
    // Execution data TTL (24 hours)
    private static final Duration EXECUTION_TTL = Duration.ofHours(24);
    
    @Override
    public String startExecution(String workflowId, Map<String, Object> inputs) {
        String executionId = UUID.randomUUID().toString();
        return startExecution(workflowId, inputs, executionId);
    }
    
    @Override
    public String startExecution(String workflowId, Map<String, Object> inputs, String executionId) {
        Instant startTime = Instant.now();
        
        // Create execution record
        Map<String, Object> executionData = new HashMap<>();
        executionData.put("id", executionId);
        executionData.put("workflowId", workflowId);
        executionData.put("status", "running");
        executionData.put("startTime", startTime.toString());
        executionData.put("inputs", inputs);
        executionData.put("nodes", new HashMap<String, Object>());
        
        // Store in memory and Redis
        activeExecutions.put(executionId, executionData);
        redisTemplate.opsForValue().set(EXECUTION_KEY_PREFIX + executionId, executionData, EXECUTION_TTL);
        
        log.info("Started workflow execution: {} for workflow: {}", executionId, workflowId);
        return executionId;
    }
    
    @Override
    public void completeExecution(String executionId, Map<String, Object> outputs) {
        Map<String, Object> executionData = getExecutionData(executionId);
        if (executionData == null) {
            log.warn("Attempted to complete unknown execution: {}", executionId);
            return;
        }
        
        Instant startTime = Instant.parse((String) executionData.get("startTime"));
        Instant endTime = Instant.now();
        long durationMs = Duration.between(startTime, endTime).toMillis();
        
        // Update execution data
        executionData.put("status", "completed");
        executionData.put("endTime", endTime.toString());
        executionData.put("durationMs", durationMs);
        executionData.put("outputs", outputs);
        
        // Update storage
        activeExecutions.put(executionId, executionData);
        redisTemplate.opsForValue().set(EXECUTION_KEY_PREFIX + executionId, executionData, EXECUTION_TTL);
        
        // Update workflow metrics
        updateWorkflowMetrics(executionData);
        
        log.info("Completed workflow execution: {} in {}ms", executionId, durationMs);
    }
    
    @Override
    public void failExecution(String executionId, String error) {
        Map<String, Object> executionData = getExecutionData(executionId);
        if (executionData == null) {
            log.warn("Attempted to fail unknown execution: {}", executionId);
            return;
        }
        
        Instant startTime = Instant.parse((String) executionData.get("startTime"));
        Instant endTime = Instant.now();
        long durationMs = Duration.between(startTime, endTime).toMillis();
        
        // Update execution data
        executionData.put("status", "failed");
        executionData.put("endTime", endTime.toString());
        executionData.put("durationMs", durationMs);
        executionData.put("error", error);
        
        // Update storage
        activeExecutions.put(executionId, executionData);
        redisTemplate.opsForValue().set(EXECUTION_KEY_PREFIX + executionId, executionData, EXECUTION_TTL);
        
        // Update workflow metrics
        updateWorkflowMetrics(executionData);
        
        log.error("Failed workflow execution: {} after {}ms: {}", executionId, durationMs, error);
    }
    
    @Override
    public String recordNodeStart(String executionId, String nodeId, String nodeType, Map<String, Object> inputs) {
        Map<String, Object> executionData = getExecutionData(executionId);
        if (executionData == null) {
            log.warn("Attempted to record node start for unknown execution: {}", executionId);
            return UUID.randomUUID().toString(); // Return dummy ID
        }
        
        String nodeExecutionId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        // Create node execution record
        Map<String, Object> nodeData = new HashMap<>();
        nodeData.put("id", nodeExecutionId);
        nodeData.put("executionId", executionId);
        nodeData.put("nodeId", nodeId);
        nodeData.put("nodeType", nodeType);
        nodeData.put("status", "running");
        nodeData.put("startTime", startTime.toString());
        nodeData.put("inputs", inputs);
        
        // Get nodes map from execution data
        @SuppressWarnings("unchecked")
        Map<String, Object> nodes = (Map<String, Object>) executionData.get("nodes");
        if (nodes == null) {
            nodes = new HashMap<>();
            executionData.put("nodes", nodes);
        }
        
        // Add node to execution data
        nodes.put(nodeId, nodeData);
        
        // Store node data
        redisTemplate.opsForValue().set(NODE_EXECUTION_KEY_PREFIX + nodeExecutionId, nodeData, EXECUTION_TTL);
        
        // Update execution data
        activeExecutions.put(executionId, executionData);
        redisTemplate.opsForValue().set(EXECUTION_KEY_PREFIX + executionId, executionData, EXECUTION_TTL);
        
        log.debug("Started node execution: {} (type: {}) for workflow execution: {}", 
                nodeId, nodeType, executionId);
        
        return nodeExecutionId;
    }
    
    @Override
    public void recordNodeComplete(String nodeExecutionId, Map<String, Object> outputs, Map<String, Object> metadata) {
        // Get node execution data
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) redisTemplate.opsForValue()
                .get(NODE_EXECUTION_KEY_PREFIX + nodeExecutionId);
        
        if (nodeData == null) {
            log.warn("Attempted to complete unknown node execution: {}", nodeExecutionId);
            return;
        }
        
        String executionId = (String) nodeData.get("executionId");
        String nodeId = (String) nodeData.get("nodeId");
        Instant startTime = Instant.parse((String) nodeData.get("startTime"));
        Instant endTime = Instant.now();
        long durationMs = Duration.between(startTime, endTime).toMillis();
        
        // Update node data
        nodeData.put("status", "completed");
        nodeData.put("endTime", endTime.toString());
        nodeData.put("durationMs", durationMs);
        nodeData.put("outputs", outputs);
        nodeData.put("metadata", metadata);
        
        // Store updated node data
        redisTemplate.opsForValue().set(NODE_EXECUTION_KEY_PREFIX + nodeExecutionId, nodeData, EXECUTION_TTL);
        
        // Update execution data
        Map<String, Object> executionData = getExecutionData(executionId);
        if (executionData != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nodes = (Map<String, Object>) executionData.get("nodes");
            if (nodes != null) {
                nodes.put(nodeId, nodeData);
                activeExecutions.put(executionId, executionData);
                redisTemplate.opsForValue().set(EXECUTION_KEY_PREFIX + executionId, executionData, EXECUTION_TTL);
            }
        }
        
        log.debug("Completed node execution: {} in {}ms", nodeId, durationMs);
    }
    
    @Override
    public void recordNodeError(String nodeExecutionId, String error, Map<String, Object> metadata) {
        // Get node execution data
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) redisTemplate.opsForValue()
                .get(NODE_EXECUTION_KEY_PREFIX + nodeExecutionId);
        
        if (nodeData == null) {
            log.warn("Attempted to record error for unknown node execution: {}", nodeExecutionId);
            return;
        }
        
        String executionId = (String) nodeData.get("executionId");
        String nodeId = (String) nodeData.get("nodeId");
        Instant startTime = Instant.parse((String) nodeData.get("startTime"));
        Instant endTime = Instant.now();
        long durationMs = Duration.between(startTime, endTime).toMillis();
        
        // Update node data
        nodeData.put("status", "failed");
        nodeData.put("endTime", endTime.toString());
        nodeData.put("durationMs", durationMs);
        nodeData.put("error", error);
        nodeData.put("metadata", metadata);
        
        // Store updated node data
        redisTemplate.opsForValue().set(NODE_EXECUTION_KEY_PREFIX + nodeExecutionId, nodeData, EXECUTION_TTL);
        
        // Update execution data
        Map<String, Object> executionData = getExecutionData(executionId);
        if (executionData != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nodes = (Map<String, Object>) executionData.get("nodes");
            if (nodes != null) {
                nodes.put(nodeId, nodeData);
                activeExecutions.put(executionId, executionData);
                redisTemplate.opsForValue().set(EXECUTION_KEY_PREFIX + executionId, executionData, EXECUTION_TTL);
            }
        }
        
        log.error("Node execution failed: {} after {}ms: {}", nodeId, durationMs, error);
    }
    
    @Override
    public Map<String, Object> getExecutionData(String executionId) {
        // Try memory cache first
        Map<String, Object> executionData = activeExecutions.get(executionId);
        if (executionData != null) {
            return new HashMap<>(executionData);
        }
        
        // Try Redis
        @SuppressWarnings("unchecked")
        Map<String, Object> redisData = (Map<String, Object>) redisTemplate.opsForValue()
                .get(EXECUTION_KEY_PREFIX + executionId);
        
        if (redisData != null) {
            // Add to memory cache
            activeExecutions.put(executionId, redisData);
            return new HashMap<>(redisData);
        }
        
        return null;
    }
    
    @Override
    public Map<String, Object> getWorkflowExecutionMetadata(String workflowId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) redisTemplate.opsForValue()
                .get(WORKFLOW_METRICS_KEY_PREFIX + workflowId);
        
        return metrics != null ? metrics : new HashMap<>();
    }
    
    /**
     * Update workflow metrics based on execution data
     */
    private void updateWorkflowMetrics(Map<String, Object> executionData) {
        String workflowId = (String) executionData.get("workflowId");
        String status = (String) executionData.get("status");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) redisTemplate.opsForValue()
                .get(WORKFLOW_METRICS_KEY_PREFIX + workflowId);
        
        if (metrics == null) {
            metrics = new HashMap<>();
            metrics.put("totalExecutions", 0);
            metrics.put("completedExecutions", 0);
            metrics.put("failedExecutions", 0);
            metrics.put("totalExecutionTimeMs", 0L);
            metrics.put("averageExecutionTimeMs", 0.0);
            metrics.put("lastExecutionId", "");
            metrics.put("lastExecutionTime", "");
        }
        
        // Update metrics
        metrics.put("totalExecutions", ((Number) metrics.get("totalExecutions")).intValue() + 1);
        
        if ("completed".equals(status)) {
            metrics.put("completedExecutions", ((Number) metrics.get("completedExecutions")).intValue() + 1);
        } else if ("failed".equals(status)) {
            metrics.put("failedExecutions", ((Number) metrics.get("failedExecutions")).intValue() + 1);
        }
        
        if (executionData.containsKey("durationMs")) {
            long totalTime = ((Number) metrics.get("totalExecutionTimeMs")).longValue() 
                    + ((Number) executionData.get("durationMs")).longValue();
            
            metrics.put("totalExecutionTimeMs", totalTime);
            
            double avgTime = (double) totalTime / ((Number) metrics.get("totalExecutions")).intValue();
            metrics.put("averageExecutionTimeMs", avgTime);
        }
        
        metrics.put("lastExecutionId", executionData.get("id"));
        metrics.put("lastExecutionTime", executionData.get("endTime"));
        
        // Store updated metrics
        redisTemplate.opsForValue().set(WORKFLOW_METRICS_KEY_PREFIX + workflowId, metrics);
    }
    
    @Override
    public Map<String, Object> getExecutionResult(String executionId) {
        Map<String, Object> executionData = getExecutionData(executionId);
        if (executionData == null) {
            log.warn("Attempted to get result for unknown execution: {}", executionId);
            return new HashMap<>();
        }
        
        String status = (String) executionData.get("status");
        
        // If execution is completed, return the outputs
        if ("completed".equals(status)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputs = (Map<String, Object>) executionData.get("outputs");
            if (outputs != null) {
                return outputs;
            }
        }
        
        // If execution failed, return error information
        if ("failed".equals(status)) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", executionData.get("error"));
            result.put("status", "failed");
            return result;
        }
        
        // If execution is still running, return current status
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("message", "Execution is still in progress");
        return result;
    }
    
    @Override
    public boolean isExecutionComplete(String executionId) {
        Map<String, Object> executionData = getExecutionData(executionId);
        if (executionData == null) {
            return false;
        }
        
        String status = (String) executionData.get("status");
        return "completed".equals(status) || "failed".equals(status);
    }
} 