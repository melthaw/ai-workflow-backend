package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.service.StreamManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * SSE流管理服务实现
 * 提供高效的流处理和连接管理功能
 */
@Slf4j
@Service
public class StreamManagerServiceImpl implements StreamManagerService {

    // 缓存活跃的发射器
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    
    // 用于心跳和清理任务的调度器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // 最大空闲时间（毫秒）
    private static final long MAX_IDLE_TIME = 180000; // 3分钟
    
    public StreamManagerServiceImpl() {
        // 启动定期检查和清理任务
        scheduler.scheduleAtFixedRate(this::cleanupEmitters, 60, 60, TimeUnit.SECONDS);
    }
    
    @Override
    public SseEmitter createEmitter(String sessionId, long timeout) {
        // 创建新的发射器
        SseEmitter emitter = new SseEmitter(timeout);
        
        // 设置完成回调
        emitter.onCompletion(() -> {
            log.debug("SSE stream completed for session: {}", sessionId);
            activeEmitters.remove(sessionId);
        });
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE stream timeout for session: {}", sessionId);
            activeEmitters.remove(sessionId);
        });
        
        // 设置错误回调
        emitter.onError(e -> {
            log.error("SSE stream error for session: {}", sessionId, e);
            activeEmitters.remove(sessionId);
        });
        
        // 保存到活跃的发射器map
        activeEmitters.put(sessionId, emitter);
        
        // 启动心跳任务
        startHeartbeat(sessionId);
        
        return emitter;
    }
    
    @Override
    public void streamWorkflow(String sessionId, BiConsumer<String, Boolean> chunkConsumer) {
        SseEmitter emitter = activeEmitters.get(sessionId);
        if (emitter == null) {
            log.warn("No active emitter found for session: {}", sessionId);
            return;
        }
        
        // 创建包装函数处理消息发送
        BiConsumer<String, Boolean> wrappedConsumer = (chunk, isLast) -> {
            try {
                if (chunk != null && !chunk.isEmpty()) {
                    emitter.send(SseEmitter.event()
                        .name("chunk")
                        .data(chunk));
                }
                
                if (isLast) {
                    emitter.send(SseEmitter.event()
                        .name("done")
                        .data(""));
                    emitter.complete();
                    activeEmitters.remove(sessionId);
                }
            } catch (IOException e) {
                log.error("Error sending SSE event", e);
                emitter.completeWithError(e);
                activeEmitters.remove(sessionId);
            }
        };
        
        // 传递包装后的消费者
        chunkConsumer.accept("", false);
    }
    
    @Override
    public void sendEvent(String sessionId, String eventName, Object data) {
        SseEmitter emitter = activeEmitters.get(sessionId);
        if (emitter == null) {
            log.warn("No active emitter found for session: {}", sessionId);
            return;
        }
        
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
        } catch (IOException e) {
            log.error("Error sending SSE event", e);
            emitter.completeWithError(e);
            activeEmitters.remove(sessionId);
        }
    }
    
    @Override
    public void completeEmitter(String sessionId) {
        SseEmitter emitter = activeEmitters.get(sessionId);
        if (emitter != null) {
            emitter.complete();
            activeEmitters.remove(sessionId);
        }
    }
    
    @Override
    public void completeEmitterWithError(String sessionId, Exception error) {
        SseEmitter emitter = activeEmitters.get(sessionId);
        if (emitter != null) {
            emitter.completeWithError(error);
            activeEmitters.remove(sessionId);
        }
    }
    
    @Override
    public boolean isEmitterActive(String sessionId) {
        return activeEmitters.containsKey(sessionId);
    }
    
    /**
     * 启动心跳任务，保持连接活跃
     */
    private void startHeartbeat(String sessionId) {
        scheduler.scheduleAtFixedRate(() -> {
            SseEmitter emitter = activeEmitters.get(sessionId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(""));
                } catch (IOException e) {
                    log.warn("Failed to send heartbeat, closing connection: {}", sessionId);
                    emitter.completeWithError(e);
                    activeEmitters.remove(sessionId);
                }
            } else {
                // 如果发射器已经不存在，取消该任务
                throw new RuntimeException("Emitter not found, cancelling heartbeat task");
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 清理空闲的发射器
     */
    private void cleanupEmitters() {
        log.debug("Running emitter cleanup task, active emitters: {}", activeEmitters.size());
        // 实际实现应检查每个发射器的最后活动时间
        // 此处简化实现
    }
} 