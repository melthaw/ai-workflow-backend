package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.response.ApiResponse;
import com.fastgpt.ai.service.WorkflowMonitorService;
import com.fastgpt.ai.service.WorkflowStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for streaming workflow execution
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflows/stream")
@Tag(name = "Workflow Streaming", description = "API for streaming workflow execution")
@RequiredArgsConstructor
public class WorkflowStreamController {

    private final WorkflowStreamService workflowStreamService;
    private final WorkflowMonitorService workflowMonitorService;
    
    @Operation(summary = "Stream workflow execution", description = "Execute a workflow and stream results using Server-Sent Events")
    @PostMapping(value = "/{workflowId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWorkflow(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, Object> inputs) {
        
        // Create SseEmitter with extended timeout
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes
        
        // Normalize inputs
        Map<String, Object> normalizedInputs = inputs != null ? inputs : new HashMap<>();
        
        // Start streaming workflow execution
        workflowStreamService.streamWorkflowToEmitter(workflowId, normalizedInputs, emitter);
        
        return emitter;
    }
    
    @Operation(summary = "Cancel streaming execution", description = "Cancel an ongoing streaming workflow execution")
    @PostMapping("/{executionId}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelStreaming(@PathVariable String executionId) {
        boolean cancelled = workflowStreamService.cancelStreaming(executionId);
        
        if (cancelled) {
            return ResponseEntity.ok(ApiResponse.success("Streaming execution cancelled successfully", true));
        } else {
            return ResponseEntity.ok(ApiResponse.failure("Failed to cancel streaming execution", false));
        }
    }
    
    @Operation(summary = "Stream workflow execution with callbacks", description = "Execute a workflow asynchronously with streaming and return a token to check status")
    @PostMapping("/{workflowId}/async")
    public ResponseEntity<ApiResponse<Map<String, String>>> streamWorkflowAsync(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, Object> inputs) {
        
        // Normalize inputs
        Map<String, Object> normalizedInputs = inputs != null ? inputs : new HashMap<>();
        
        // Generate a unique execution ID
        String executionId = java.util.UUID.randomUUID().toString();
        normalizedInputs.put("executionId", executionId);
        
        // Execute workflow asynchronously
        CompletableFuture<Map<String, Object>> future = workflowStreamService.executeStreamingWorkflow(
                workflowId,
                normalizedInputs,
                (chunk, isComplete) -> {
                    // Chunks are handled internally and can be retrieved later
                    log.debug("Received chunk for execution {}: {} chars, complete: {}", 
                            executionId, chunk != null ? chunk.length() : 0, isComplete);
                }
        );
        
        // Return execution ID for status checks
        Map<String, String> response = new HashMap<>();
        response.put("executionId", executionId);
        response.put("status", "running");
        
        return ResponseEntity.ok(ApiResponse.success("Workflow execution started", response));
    }
    
    @Operation(summary = "Check async execution status", description = "Get the current status and result of an asynchronous workflow execution")
    @GetMapping("/{executionId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionStatus(@PathVariable String executionId) {
        // Check if execution is complete
        boolean isComplete = workflowMonitorService.isExecutionComplete(executionId);
        
        // Get execution data
        Map<String, Object> executionData = workflowMonitorService.getExecutionData(executionId);
        
        if (executionData == null || executionData.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.failure("Execution not found", Map.of(
                    "executionId", executionId,
                    "status", "not_found"
            )));
        }
        
        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", executionId);
        response.put("status", executionData.get("status"));
        
        // Add result if available
        if (isComplete) {
            Map<String, Object> result = workflowMonitorService.getExecutionResult(executionId);
            response.put("result", result);
            
            // Add timing information
            if (executionData.containsKey("startTime")) {
                response.put("startTime", executionData.get("startTime"));
            }
            if (executionData.containsKey("endTime")) {
                response.put("endTime", executionData.get("endTime"));
            }
            if (executionData.containsKey("durationMs")) {
                response.put("durationMs", executionData.get("durationMs"));
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
} 