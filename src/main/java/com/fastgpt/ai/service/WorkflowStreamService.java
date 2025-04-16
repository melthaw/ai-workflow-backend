package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.workflow.StreamProgressUpdate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Service for handling streaming workflow executions
 */
public interface WorkflowStreamService {
    
    /**
     * Execute a workflow with SSE streaming
     * @param workflowId Workflow ID
     * @param inputs Input map
     * @param emitter SSE emitter for streaming responses
     * @return CompletableFuture that completes when workflow is done
     */
    CompletableFuture<Void> streamWorkflowToEmitter(String workflowId, Map<String, Object> inputs, SseEmitter emitter);
    
    /**
     * Execute workflow with callback for text chunks
     * @param workflowId Workflow ID
     * @param inputs Input map
     * @param chunkConsumer Consumer for text chunks (text, isComplete)
     * @return CompletableFuture that completes when workflow is done
     */
    CompletableFuture<Map<String, Object>> executeStreamingWorkflow(String workflowId, Map<String, Object> inputs, 
                                                      BiConsumer<String, Boolean> chunkConsumer);
    
    /**
     * Execute workflow with callback for progress updates
     * @param workflowId Workflow ID
     * @param inputs Input map 
     * @param progressConsumer Consumer for progress updates
     * @return CompletableFuture that completes when workflow is done
     */
    CompletableFuture<Map<String, Object>> executeWithProgressUpdates(String workflowId, Map<String, Object> inputs,
                                                       Consumer<StreamProgressUpdate> progressConsumer);
    
    /**
     * Send heartbeat to SSE emitter
     * @param emitter SSE emitter
     * @return CompletableFuture that completes when heartbeat is done
     */
    CompletableFuture<Void> startHeartbeat(SseEmitter emitter);
    
    /**
     * Cancel streaming for a workflow execution
     * @param executionId Execution ID
     * @return True if successfully cancelled
     */
    boolean cancelStreaming(String executionId);
} 