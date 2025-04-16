package com.fastgpt.ai.dto.workflow;

import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Context for streaming workflow execution
 */
@Data
public class StreamContext {
    
    /**
     * Unique ID for this stream
     */
    private final String streamId;
    
    /**
     * ID of the workflow being executed
     */
    private final String workflowId;
    
    /**
     * ID of the execution
     */
    private final String executionId;
    
    /**
     * The SSE emitter for this stream
     */
    private final SseEmitter emitter;
    
    /**
     * Chunk consumer for handling text output
     */
    private final BiConsumer<String, Boolean> chunkConsumer;
    
    /**
     * The time this stream was created
     */
    private final LocalDateTime createdAt;
    
    /**
     * Last activity time for this stream
     */
    private LocalDateTime lastActivityAt;
    
    /**
     * Whether this stream is complete
     */
    private boolean completed;
    
    /**
     * Execution context / variables
     */
    private final Map<String, Object> context;
    
    /**
     * Create a new stream context
     */
    public StreamContext(String streamId, String workflowId, String executionId, SseEmitter emitter, 
                        BiConsumer<String, Boolean> chunkConsumer) {
        this.streamId = streamId;
        this.workflowId = workflowId;
        this.executionId = executionId;
        this.emitter = emitter;
        this.chunkConsumer = chunkConsumer;
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = this.createdAt;
        this.completed = false;
        this.context = new ConcurrentHashMap<>();
    }
    
    /**
     * Update the last activity time
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    /**
     * Mark the stream as completed
     */
    public void complete() {
        this.completed = true;
        this.updateActivity();
    }
    
    /**
     * Check if the stream is inactive for too long
     * @param timeoutMinutes Timeout in minutes
     * @return true if the stream has timed out
     */
    public boolean hasTimedOut(int timeoutMinutes) {
        return !completed && 
                LocalDateTime.now().isAfter(
                        lastActivityAt.plusMinutes(timeoutMinutes));
    }
    
    /**
     * Put a value in the context
     */
    public void putContext(String key, Object value) {
        context.put(key, value);
        updateActivity();
    }
    
    /**
     * Get a value from the context
     */
    public Object getContext(String key) {
        return context.get(key);
    }
    
    /**
     * Remove a value from the context
     */
    public Object removeContext(String key) {
        updateActivity();
        return context.remove(key);
    }
    
    /**
     * Clear the context
     */
    public void clearContext() {
        context.clear();
        updateActivity();
    }
} 