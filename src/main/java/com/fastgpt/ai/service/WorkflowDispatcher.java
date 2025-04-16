package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.WorkflowDebugResponse;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 工作流调度器接口，负责整个工作流的执行过程
 * 对标Next.js版本中的dispatchWorkFlow函数
 */
public interface WorkflowDispatcher {
    
    /**
     * 调度执行工作流
     * 
     * @param workflow 要执行的工作流
     * @param inputs 输入参数
     * @param userId 用户ID
     * @param teamId 团队ID
     * @param appId 应用ID
     * @param streamConsumer 流式输出消费者(chunk, isLast)
     * @return 工作流执行结果
     */
    Map<String, Object> dispatchWorkflow(
        WorkflowDTO workflow,
        Map<String, Object> inputs,
        String userId,
        String teamId,
        String appId,
        BiConsumer<String, Boolean> streamConsumer
    );
    
    /**
     * 调试工作流执行
     * 
     * @param workflow 要调试的工作流
     * @param inputs 输入参数
     * @param userId 用户ID
     * @param teamId 团队ID
     * @param appId 应用ID
     * @return 调试信息
     */
    WorkflowDebugResponse debugWorkflow(
        WorkflowDTO workflow,
        Map<String, Object> inputs, 
        String userId,
        String teamId,
        String appId
    );
} 