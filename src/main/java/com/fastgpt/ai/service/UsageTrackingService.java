package com.fastgpt.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 资源使用统计服务接口
 * 对标Next.js版本的createChatUsage功能
 */
public interface UsageTrackingService {
    
    /**
     * 跟踪工作流使用情况
     * @param appName 应用名称
     * @param appId 应用ID
     * @param teamId 团队ID
     * @param userId 用户ID
     * @param source 使用来源
     * @param flowUsages 使用情况记录列表
     */
    void trackWorkflowUsage(
        String appName,
        String appId,
        String teamId,
        String userId,
        String source,
        List<Map<String, Object>> flowUsages
    );
    
    /**
     * 跟踪AI模型使用情况
     * @param appId 应用ID
     * @param teamId 团队ID
     * @param userId 用户ID
     * @param model 模型名称
     * @param usage 使用情况
     */
    void trackModelUsage(
        String appId,
        String teamId,
        String userId,
        String model,
        Map<String, Object> usage
    );
    
    /**
     * 跟踪节点执行情况
     * @param appId 应用ID
     * @param teamId 团队ID
     * @param userId 用户ID
     * @param nodeType 节点类型
     * @param executionTimeMs 执行时间(毫秒)
     */
    void trackNodeExecution(
        String appId,
        String teamId,
        String userId,
        String nodeType,
        long executionTimeMs
    );
} 