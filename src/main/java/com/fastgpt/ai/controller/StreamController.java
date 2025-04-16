package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.workflow.StreamRequest;
import com.fastgpt.ai.service.StreamService;
import com.fastgpt.ai.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Controller for streaming workflow execution
 */
@Slf4j
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Tag(name = "Stream API", description = "API for streaming workflow execution")
public class StreamController {
    
    private final StreamService streamService;
    private final WorkflowService workflowService;
    
    @Operation(summary = "Stream workflow execution", description = "Execute a workflow with SSE streaming")
    @PostMapping(value = "/workflow/{workflowId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWorkflow(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> inputs) {
        
        log.info("Starting streaming workflow execution: {} with inputs: {}", workflowId, inputs);
        return streamService.streamWorkflowExecution(workflowId, inputs);
    }
    
    @Operation(summary = "Stream workflow execution with config", description = "Execute a workflow with SSE streaming and additional configuration")
    @PostMapping(value = "/workflow/{workflowId}/config", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWorkflowWithConfig(
            @PathVariable String workflowId,
            @RequestBody StreamRequest request) {
        
        log.info("Starting streaming workflow execution with config: {} with inputs: {}", workflowId, request);
        
        // Apply any stream-specific configuration before executing
        // For example, set streaming flags in the inputs
        Map<String, Object> inputs = request.getInputs();
        inputs.put("__streaming", true);
        
        if (request.getConfig() != null) {
            inputs.put("__config", request.getConfig());
        }
        
        return streamService.streamWorkflowExecution(workflowId, inputs);
    }
    
    @Operation(summary = "Debug-stream workflow execution", description = "Execute a workflow with SSE streaming and detailed debug information")
    @PostMapping(value = "/workflow/{workflowId}/debug", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter debugStreamWorkflow(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> inputs) {
        
        log.info("Starting debug streaming workflow execution: {} with inputs: {}", workflowId, inputs);
        
        // Add debug flag to inputs
        inputs.put("__debug", true);
        inputs.put("__streaming", true);
        
        return streamService.streamWorkflowExecution(workflowId, inputs);
    }
} 