package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.service.StreamService;
import com.fastgpt.ai.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamServiceImpl implements StreamService {
    
    private final WorkflowService workflowService;
    
    // Default timeout: 30 minutes
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L;
    // Default heartbeat interval: 15 seconds
    private static final long HEARTBEAT_INTERVAL = 15 * 1000L;
    
    // Store active emitters for cleanup if needed
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    // Task scheduler for heartbeats
    private final TaskScheduler taskScheduler = createTaskScheduler();
    
    private TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("sse-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
    
    @Override
    public SseEmitter createEmitter() {
        return createEmitter(DEFAULT_TIMEOUT);
    }
    
    @Override
    public SseEmitter createEmitter(long timeoutMs) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(timeoutMs);
        
        // Store emitter and set completion callbacks
        activeEmitters.put(emitterId, emitter);
        
        emitter.onCompletion(() -> {
            activeEmitters.remove(emitterId);
            log.debug("SSE emitter completed: {}", emitterId);
        });
        
        emitter.onTimeout(() -> {
            activeEmitters.remove(emitterId);
            log.debug("SSE emitter timed out: {}", emitterId);
        });
        
        emitter.onError(e -> {
            activeEmitters.remove(emitterId);
            log.error("SSE emitter error: {}", emitterId, e);
        });
        
        // Start heartbeat to keep connection alive
        startHeartbeat(emitter, HEARTBEAT_INTERVAL);
        
        return emitter;
    }
    
    @Override
    public boolean sendData(SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .data(data, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            log.error("Failed to send data to SSE emitter", e);
            completeWithError(emitter, e);
            return false;
        }
    }
    
    @Override
    public boolean sendData(SseEmitter emitter, String eventType, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            log.error("Failed to send data to SSE emitter with event type: {}", eventType, e);
            completeWithError(emitter, e);
            return false;
        }
    }
    
    @Override
    public Runnable startHeartbeat(SseEmitter emitter, long intervalMs) {
        Runnable heartbeatTask = () -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping", MediaType.TEXT_PLAIN));
            } catch (IOException e) {
                // If we can't send a heartbeat, the connection is likely broken
                log.debug("Heartbeat failed, completing emitter", e);
                emitter.complete();
            }
        };
        
        // Schedule the heartbeat task
        taskScheduler.scheduleAtFixedRate(
                heartbeatTask, 
                Duration.ofMillis(intervalMs)
        );
        
        // Return a runnable that can be used to stop the heartbeat
        return heartbeatTask;
    }
    
    @Override
    public void complete(SseEmitter emitter) {
        emitter.complete();
    }
    
    @Override
    public void complete(SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(data, MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (IOException e) {
            log.error("Failed to send completion data to SSE emitter", e);
            emitter.complete();
        }
    }
    
    @Override
    public void completeWithError(SseEmitter emitter, Throwable error) {
        emitter.completeWithError(error);
    }
    
    @Override
    public SseEmitter streamOperation(Object initialData, Runnable operation, Consumer<Object> onProgress) {
        SseEmitter emitter = createEmitter();
        
        // Send initial data if provided
        if (initialData != null) {
            sendData(emitter, "start", initialData);
        }
        
        // Execute the operation asynchronously
        Thread operationThread = new Thread(() -> {
            try {
                // Create a progress consumer that sends updates through the emitter
                Consumer<Object> progressHandler = progress -> {
                    if (progress != null) {
                        sendData(emitter, "progress", progress);
                    }
                    
                    // Also propagate to the original consumer if provided
                    if (onProgress != null) {
                        onProgress.accept(progress);
                    }
                };
                
                // Run the operation
                operation.run();
                
                // Signal completion
                complete(emitter, Map.of("status", "completed"));
            } catch (Exception e) {
                log.error("Error during streaming operation", e);
                completeWithError(emitter, e);
            }
        });
        
        operationThread.setDaemon(true);
        operationThread.start();
        
        return emitter;
    }
    
    @Override
    public SseEmitter streamWorkflowExecution(String workflowId, Map<String, Object> inputs) {
        SseEmitter emitter = createEmitter();
        
        // Send initial acknowledgment
        sendData(emitter, "start", Map.of(
                "workflowId", workflowId,
                "status", "started"
        ));
        
        // Execute the workflow in a separate thread
        Thread workflowThread = new Thread(() -> {
            try {
                // Create a progress consumer that will send updates to the client
                Consumer<Map<String, Object>> progressConsumer = progress -> 
                    sendData(emitter, "progress", progress);
                
                // Execute the workflow with streaming
                Map<String, Object> result = workflowService.executeWorkflowWithStream(
                        workflowId, inputs, progressConsumer);
                
                // Complete the emitter with the final result
                complete(emitter, Map.of(
                        "status", "completed",
                        "result", result
                ));
            } catch (Exception e) {
                log.error("Error during workflow execution: {}", workflowId, e);
                sendData(emitter, "error", Map.of(
                        "message", e.getMessage(),
                        "type", e.getClass().getSimpleName()
                ));
                completeWithError(emitter, e);
            }
        });
        
        workflowThread.setDaemon(true);
        workflowThread.start();
        
        return emitter;
    }
} 