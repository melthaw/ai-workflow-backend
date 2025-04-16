package com.fastgpt.ai.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Service for managing streaming responses
 */
public interface StreamService {
    
    /**
     * Create a new SSE emitter with default timeout
     * @return A new SseEmitter instance
     */
    SseEmitter createEmitter();
    
    /**
     * Create a new SSE emitter with custom timeout
     * @param timeoutMs Timeout in milliseconds
     * @return A new SseEmitter instance
     */
    SseEmitter createEmitter(long timeoutMs);
    
    /**
     * Send data to an SSE emitter
     * @param emitter The SSE emitter
     * @param data The data to send
     * @return true if the data was sent successfully, false otherwise
     */
    boolean sendData(SseEmitter emitter, Object data);
    
    /**
     * Send data with an event type to an SSE emitter
     * @param emitter The SSE emitter
     * @param eventType The event type
     * @param data The data to send
     * @return true if the data was sent successfully, false otherwise
     */
    boolean sendData(SseEmitter emitter, String eventType, Object data);
    
    /**
     * Start sending heartbeat events to an emitter
     * @param emitter The SSE emitter
     * @param intervalMs Interval between heartbeats in milliseconds
     * @return A runnable that can be used to stop the heartbeat
     */
    Runnable startHeartbeat(SseEmitter emitter, long intervalMs);
    
    /**
     * Complete an SSE emitter
     * @param emitter The SSE emitter
     */
    void complete(SseEmitter emitter);
    
    /**
     * Complete an SSE emitter with data
     * @param emitter The SSE emitter
     * @param data The completion data
     */
    void complete(SseEmitter emitter, Object data);
    
    /**
     * Complete an SSE emitter with an error
     * @param emitter The SSE emitter
     * @param error The error
     */
    void completeWithError(SseEmitter emitter, Throwable error);
    
    /**
     * Performs an asynchronous streaming operation with progress updates
     * @param initialData Initial data to send
     * @param operation The operation to perform
     * @param onProgress Callback for progress updates
     * @return An SseEmitter for the stream
     */
    SseEmitter streamOperation(Object initialData, 
                               Runnable operation, 
                               Consumer<Object> onProgress);
    
    /**
     * Stream a workflow execution
     * @param workflowId Workflow ID
     * @param inputs Input variables
     * @return An SseEmitter for the stream
     */
    SseEmitter streamWorkflowExecution(String workflowId, Map<String, Object> inputs);
} 