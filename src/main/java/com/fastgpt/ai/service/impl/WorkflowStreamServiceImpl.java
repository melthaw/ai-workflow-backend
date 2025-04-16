package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.dto.workflow.StreamProgressUpdate;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.exception.StreamingException;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.WorkflowMonitorService;
import com.fastgpt.ai.service.WorkflowService;
import com.fastgpt.ai.service.WorkflowStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Implementation of workflow streaming service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowStreamServiceImpl implements WorkflowStreamService {

    private final WorkflowService workflowService;
    private final WorkflowMonitorService monitorService;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2);
    
    // Active emitters by execution ID
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    // Cancellation flags by execution ID 
    private final Map<String, Boolean> cancellationFlags = new ConcurrentHashMap<>();
    
    // Constants
    private static final long HEARTBEAT_INTERVAL_MS = 10000; // 10 seconds
    private static final long MAX_HEARTBEAT_COUNT = 300; // 5 minutes (10s * 300)
    private static final long SSE_TIMEOUT = 300 * 1000; // 5 minutes
    
    @Override
    @Async
    public CompletableFuture<Void> streamWorkflowToEmitter(String workflowId, Map<String, Object> inputs, SseEmitter emitter) {
        String executionId = UUID.randomUUID().toString();
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        
        try {
            // Configure emitter for long timeout
            emitter.onTimeout(() -> {
                log.warn("SSE connection timed out for execution: {}", executionId);
                cleanupResources(executionId);
                completionFuture.complete(null);
            });
            
            emitter.onCompletion(() -> {
                log.info("SSE connection completed for execution: {}", executionId);
                cleanupResources(executionId);
                completionFuture.complete(null);
            });
            
            emitter.onError(ex -> {
                log.error("SSE connection error for execution: {}", executionId, ex);
                cleanupResources(executionId);
                completionFuture.completeExceptionally(ex);
            });
            
            // Store active emitter for potential cancellation
            activeEmitters.put(executionId, emitter);
            cancellationFlags.put(executionId, false);
            
            // Send start event
            sendEvent(emitter, "start", StreamProgressUpdate.builder()
                    .executionId(executionId)
                    .workflowId(workflowId)
                    .updateType(StreamProgressUpdate.UpdateType.CHUNK)
                    .content("Workflow execution started")
                    .complete(false)
                    .build());
            
            // Start heartbeat
            startHeartbeat(emitter);
            
            // Execute workflow with streaming
            executeWithProgressUpdates(workflowId, inputs, progressUpdate -> {
                try {
                    // Check if cancelled
                    if (cancellationFlags.getOrDefault(executionId, false)) {
                        throw new StreamingException("Workflow execution cancelled by user");
                    }
                    
                    // Determine event name based on update type
                    String eventName;
                    switch (progressUpdate.getUpdateType()) {
                        case CHUNK:
                            eventName = "chunk";
                            break;
                        case NODE_COMPLETE:
                            eventName = "node";
                            break;
                        case EDGE_UPDATE:
                            eventName = "edge";
                            break;
                        case ERROR:
                            eventName = "error";
                            break;
                        case COMPLETE:
                            eventName = "complete";
                            break;
                        default:
                            eventName = "update";
                    }
                    
                    // Send the event
                    sendEvent(emitter, eventName, progressUpdate);
                    
                    // If complete, close the emitter
                    if (progressUpdate.isComplete()) {
                        log.info("Workflow execution complete, closing emitter for {}", executionId);
                        cleanupResources(executionId);
                        emitter.complete();
                        completionFuture.complete(null);
                    }
                } catch (Exception e) {
                    log.error("Error sending SSE event for execution: {}", executionId, e);
                    try {
                        sendEvent(emitter, "error", StreamProgressUpdate.builder()
                                .executionId(executionId)
                                .workflowId(workflowId)
                                .updateType(StreamProgressUpdate.UpdateType.ERROR)
                                .errorMessage(e.getMessage())
                                .complete(true)
                                .build());
                        cleanupResources(executionId);
                        emitter.complete();
                    } catch (Exception ex) {
                        // Ignore, connection likely closed
                    }
                    completionFuture.completeExceptionally(e);
                }
            }).exceptionally(ex -> {
                log.error("Error executing workflow: {}", workflowId, ex);
                try {
                    sendEvent(emitter, "error", StreamProgressUpdate.builder()
                            .executionId(executionId)
                            .workflowId(workflowId)
                            .updateType(StreamProgressUpdate.UpdateType.ERROR)
                            .errorMessage(ex.getMessage())
                            .complete(true)
                            .build());
                    cleanupResources(executionId);
                    emitter.complete();
                } catch (Exception e) {
                    // Ignore, connection likely closed
                }
                completionFuture.completeExceptionally(ex);
                return null;
            });
            
        } catch (Exception e) {
            log.error("Error setting up SSE for workflow: {}", workflowId, e);
            cleanupResources(executionId);
            completionFuture.completeExceptionally(e);
        }
        
        return completionFuture;
    }
    
    @Override
    @Async
    public CompletableFuture<Map<String, Object>> executeStreamingWorkflow(String workflowId, Map<String, Object> inputs, 
                                                           BiConsumer<String, Boolean> chunkConsumer) {
        CompletableFuture<Map<String, Object>> resultFuture = new CompletableFuture<>();
        String executionId = monitorService.startExecution(workflowId, inputs);
        final Map<String, Object> result = new HashMap<>();
        
        try {
            // Get workflow
            WorkflowDTO workflow = workflowService.getWorkflowById(workflowId);
            
            // Execute with progress updates
            return executeWithProgressUpdates(workflowId, inputs, update -> {
                // Only send actual content chunks to the consumer
                if (update.getUpdateType() == StreamProgressUpdate.UpdateType.CHUNK && update.getContent() != null) {
                    chunkConsumer.accept(update.getContent(), update.isComplete());
                }
                
                // If complete, store the final result
                if (update.isComplete() && update.getUpdateType() == StreamProgressUpdate.UpdateType.COMPLETE) {
                    result.putAll(monitorService.getExecutionResult(executionId));
                }
            }).thenApply(finalResult -> {
                // Return the result
                monitorService.completeExecution(executionId, finalResult);
                return finalResult;
            }).exceptionally(ex -> {
                log.error("Error executing streaming workflow: {}", workflowId, ex);
                String errorMsg = ex.getMessage();
                if (ex.getCause() != null) {
                    errorMsg = ex.getCause().getMessage();
                }
                monitorService.failExecution(executionId, errorMsg);
                chunkConsumer.accept("Error: " + errorMsg, true);
                throw new WorkflowExecutionException("Error executing workflow: " + errorMsg, ex);
            });
        } catch (Exception e) {
            log.error("Error preparing streaming workflow: {}", workflowId, e);
            monitorService.failExecution(executionId, e.getMessage());
            resultFuture.completeExceptionally(e);
            return resultFuture;
        }
    }

    @Override
    @Async
    public CompletableFuture<Map<String, Object>> executeWithProgressUpdates(String workflowId, Map<String, Object> inputs,
                                                            Consumer<StreamProgressUpdate> progressConsumer) {
        CompletableFuture<Map<String, Object>> resultFuture = new CompletableFuture<>();
        
        try {
            // Generate unique execution ID if not provided in inputs
            String executionId = inputs.containsKey("executionId") 
                    ? inputs.get("executionId").toString() 
                    : UUID.randomUUID().toString();
            
            // Register execution in monitor
            monitorService.startExecution(workflowId, inputs, executionId);
            
            // Execute workflow asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    // Get workflow
                    WorkflowDTO workflow = workflowService.getWorkflowById(workflowId);
                    
                    // Create an output collector consumer
                    final StringBuilder outputBuilder = new StringBuilder();
                    BiConsumer<String, Boolean> chunkCollector = (chunk, isComplete) -> {
                        // Add the chunk to the current output
                        if (chunk != null && !chunk.isEmpty()) {
                            outputBuilder.append(chunk);
                            
                            // Send the progress update
                            progressConsumer.accept(StreamProgressUpdate.builder()
                                    .executionId(executionId)
                                    .workflowId(workflowId)
                                    .updateType(StreamProgressUpdate.UpdateType.CHUNK)
                                    .content(chunk)
                                    .complete(isComplete)
                                    .build());
                        }
                    };
                    
                    // Execute workflow with streaming
                    workflowService.streamWorkflow(workflowId, inputs, chunkCollector);
                    
                    // Get final execution result
                    Map<String, Object> result = monitorService.getExecutionResult(executionId);
                    
                    // Add the complete output to result if not already there
                    if (!result.containsKey("output") && outputBuilder.length() > 0) {
                        result.put("output", outputBuilder.toString());
                    }
                    
                    // Send completion event
                    progressConsumer.accept(StreamProgressUpdate.builder()
                            .executionId(executionId)
                            .workflowId(workflowId)
                            .updateType(StreamProgressUpdate.UpdateType.COMPLETE)
                            .complete(true)
                            .build());
                    
                    // Complete the future with the result
                    resultFuture.complete(result);
                    
                } catch (Exception e) {
                    log.error("Error executing workflow with progress updates: {}", workflowId, e);
                    
                    // Send error event
                    progressConsumer.accept(StreamProgressUpdate.builder()
                            .executionId(executionId)
                            .workflowId(workflowId)
                            .updateType(StreamProgressUpdate.UpdateType.ERROR)
                            .errorMessage(e.getMessage())
                            .complete(true)
                            .build());
                    
                    // Fail the execution
                    monitorService.failExecution(executionId, e.getMessage());
                    
                    // Complete the future exceptionally
                    resultFuture.completeExceptionally(e);
                }
            });
            
        } catch (Exception e) {
            log.error("Error preparing workflow with progress updates: {}", workflowId, e);
            resultFuture.completeExceptionally(e);
        }
        
        return resultFuture;
    }

    @Override
    public CompletableFuture<Void> startHeartbeat(SseEmitter emitter) {
        CompletableFuture<Void> heartbeatFuture = new CompletableFuture<>();
        
        // Schedule heartbeat task
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                // Send heartbeat event
                StreamProgressUpdate heartbeat = StreamProgressUpdate.builder()
                        .updateType(StreamProgressUpdate.UpdateType.HEARTBEAT)
                        .build();
                
                sendEvent(emitter, "heartbeat", heartbeat);
            } catch (Exception e) {
                // Heartbeat failed, likely client disconnected
                heartbeatFuture.complete(null);
                throw new RuntimeException("Heartbeat failed", e);
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        return heartbeatFuture;
    }

    @Override
    public boolean cancelStreaming(String executionId) {
        // Set cancellation flag
        cancellationFlags.put(executionId, true);
        
        // Get emitter and close it if available
        SseEmitter emitter = activeEmitters.get(executionId);
        if (emitter != null) {
            try {
                // Send cancellation event
                sendEvent(emitter, "cancel", StreamProgressUpdate.builder()
                        .executionId(executionId)
                        .updateType(StreamProgressUpdate.UpdateType.ERROR)
                        .errorMessage("Execution cancelled by user")
                        .complete(true)
                        .build());
                
                // Complete the emitter
                emitter.complete();
                
                // Cleanup resources
                cleanupResources(executionId);
                
                return true;
            } catch (Exception e) {
                log.error("Error cancelling streaming for execution: {}", executionId, e);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Send SSE event with data
     */
    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
    }
    
    /**
     * Clean up resources for an execution
     */
    private void cleanupResources(String executionId) {
        activeEmitters.remove(executionId);
        cancellationFlags.remove(executionId);
    }
} 