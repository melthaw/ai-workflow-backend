package com.fastgpt.ai.service;

import java.util.Map;

/**
 * 工具服务接口
 */
public interface ToolService {
    
    /**
     * 执行工具
     *
     * @param toolId 工具ID
     * @param params 工具参数
     * @return 工具执行结果
     */
    Map<String, Object> executeTool(String toolId, Map<String, Object> params);
    
    /**
     * 获取可用工具列表
     *
     * @param userId 用户ID
     * @param teamId 团队ID
     * @return 工具列表
     */
    Map<String, Object> getAvailableTools(String userId, String teamId);
} 