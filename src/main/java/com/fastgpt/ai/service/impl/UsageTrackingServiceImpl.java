package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.service.UsageTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源使用统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageTrackingServiceImpl implements UsageTrackingService {

    @Override
    public void trackWorkflowUsage(
            String appName,
            String appId,
            String teamId,
            String userId,
            String source,
            List<Map<String, Object>> flowUsages) {
        
        if (flowUsages == null || flowUsages.isEmpty()) {
            return;
        }
        
        try {
            log.info("Tracking workflow usage for app: {}, user: {}, usages: {}", 
                    appName, userId, flowUsages.size());
            
            // 处理每个使用记录
            for (Map<String, Object> usage : flowUsages) {
                String model = getStringValue(usage, "model");
                Map<String, Object> tokens = getMapValue(usage, "tokens");
                
                // 跟踪模型使用
                if (model != null && tokens != null) {
                    trackModelUsage(appId, teamId, userId, model, tokens);
                }
                
                // 实际项目中应将使用数据保存到数据库
                // 此处仅打印日志作为示例
                log.debug("Usage tracked: app={}, team={}, user={}, model={}, tokens={}",
                        appId, teamId, userId, model, tokens);
            }
            
        } catch (Exception e) {
            log.error("Error tracking workflow usage", e);
        }
    }

    @Override
    public void trackModelUsage(
            String appId,
            String teamId,
            String userId,
            String model,
            Map<String, Object> usage) {
        
        try {
            int promptTokens = getIntValue(usage, "prompt_tokens", 0);
            int completionTokens = getIntValue(usage, "completion_tokens", 0);
            int totalTokens = getIntValue(usage, "total_tokens", 0);
            
            // 如果总token数为0，尝试从prompt和completion计算
            if (totalTokens == 0 && (promptTokens > 0 || completionTokens > 0)) {
                totalTokens = promptTokens + completionTokens;
            }
            
            // 计算成本（实际应根据模型和token数计算）
            double cost = calculateCost(model, promptTokens, completionTokens);
            
            // 创建使用记录
            Map<String, Object> usageRecord = new HashMap<>();
            usageRecord.put("appId", appId);
            usageRecord.put("teamId", teamId);
            usageRecord.put("userId", userId);
            usageRecord.put("model", model);
            usageRecord.put("promptTokens", promptTokens);
            usageRecord.put("completionTokens", completionTokens);
            usageRecord.put("totalTokens", totalTokens);
            usageRecord.put("cost", cost);
            usageRecord.put("timestamp", System.currentTimeMillis());
            
            // 实际项目中应将使用记录保存到数据库
            // 此处仅打印日志作为示例
            log.info("Model usage tracked: {}", usageRecord);
        } catch (Exception e) {
            log.error("Error tracking model usage", e);
        }
    }

    @Override
    public void trackNodeExecution(
            String appId,
            String teamId,
            String userId,
            String nodeType,
            long executionTimeMs) {
        
        try {
            // 创建执行记录
            Map<String, Object> executionRecord = new HashMap<>();
            executionRecord.put("appId", appId);
            executionRecord.put("teamId", teamId);
            executionRecord.put("userId", userId);
            executionRecord.put("nodeType", nodeType);
            executionRecord.put("executionTimeMs", executionTimeMs);
            executionRecord.put("timestamp", System.currentTimeMillis());
            
            // 实际项目中应将执行记录保存到数据库
            // 此处仅打印日志作为示例
            log.info("Node execution tracked: {}", executionRecord);
        } catch (Exception e) {
            log.error("Error tracking node execution", e);
        }
    }
    
    /**
     * 计算API调用成本
     * 实际项目中应根据最新的定价策略计算
     */
    private double calculateCost(String model, int promptTokens, int completionTokens) {
        // 简化的成本计算，实际应根据模型和定价策略计算
        double promptCost = 0;
        double completionCost = 0;
        
        if (model.contains("gpt-4")) {
            promptCost = promptTokens * 0.00003;
            completionCost = completionTokens * 0.00006;
        } else if (model.contains("gpt-3.5-turbo")) {
            promptCost = promptTokens * 0.0000015;
            completionCost = completionTokens * 0.000002;
        }
        
        return promptCost + completionCost;
    }
    
    // 辅助方法：安全获取字符串值
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : null;
    }
    
    // 辅助方法：安全获取整数值
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    // 辅助方法：安全获取Map值
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
} 