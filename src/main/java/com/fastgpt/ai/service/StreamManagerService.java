package com.fastgpt.ai.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.BiConsumer;

/**
 * SSE流管理服务接口
 * 提供统一的流处理和连接管理
 */
public interface StreamManagerService {
    
    /**
     * 创建SSE发射器
     * @param sessionId 会话ID
     * @param timeout 超时时间(毫秒)
     * @return 新创建的SSE发射器
     */
    SseEmitter createEmitter(String sessionId, long timeout);
    
    /**
     * 流式处理工作流执行结果
     * @param sessionId 会话ID
     * @param chunkConsumer 块处理函数(chunk, isLast)
     */
    void streamWorkflow(String sessionId, BiConsumer<String, Boolean> chunkConsumer);
    
    /**
     * 发送事件
     * @param sessionId 会话ID
     * @param eventName 事件名称
     * @param data 事件数据
     */
    void sendEvent(String sessionId, String eventName, Object data);
    
    /**
     * 完成发射器
     * @param sessionId 会话ID
     */
    void completeEmitter(String sessionId);
    
    /**
     * 以错误完成发射器
     * @param sessionId 会话ID
     * @param error 错误
     */
    void completeEmitterWithError(String sessionId, Exception error);
    
    /**
     * 检查发射器是否活跃
     * @param sessionId 会话ID
     * @return 是否活跃
     */
    boolean isEmitterActive(String sessionId);
} 