package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.EdgeStatusDTO;
import com.fastgpt.ai.dto.NodeExecutionMetrics;
import com.fastgpt.ai.dto.WorkflowDebugResponse;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.workflow.WorkflowInteractionState;
import com.fastgpt.ai.dto.workflow.WorkflowTemplateDTO;
import com.fastgpt.ai.dto.request.WorkflowCreateRequest;
import com.fastgpt.ai.dto.request.WorkflowUpdateRequest;
import com.fastgpt.ai.dto.response.ApiResponse;
import com.fastgpt.ai.service.WorkflowService;
import com.fastgpt.ai.service.WorkflowInteractionService;
import com.fastgpt.ai.service.WorkflowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow", description = "Workflow API")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowInteractionService workflowInteractionService;
    private final WorkflowTemplateService workflowTemplateService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowDTO>> createWorkflow(@Valid @RequestBody WorkflowCreateRequest request) {
        log.info("Creating workflow for user: {}", request.getUserId());
        WorkflowDTO createdWorkflow = workflowService.createWorkflow(request);
        return ResponseEntity.ok(ApiResponse.success(createdWorkflow));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<ApiResponse<WorkflowDTO>> getWorkflowById(@PathVariable String workflowId) {
        log.info("Getting workflow with ID: {}", workflowId);
        WorkflowDTO workflow = workflowService.getWorkflowById(workflowId);
        return ResponseEntity.ok(ApiResponse.success(workflow));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<WorkflowDTO>>> getWorkflowsByUserId(@PathVariable String userId) {
        log.info("Getting all workflows for user: {}", userId);
        List<WorkflowDTO> workflows = workflowService.getWorkflowsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(workflows));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<WorkflowDTO>>> getWorkflowsByTeamId(@PathVariable String teamId) {
        log.info("Getting all workflows for team: {}", teamId);
        List<WorkflowDTO> workflows = workflowService.getWorkflowsByTeamId(teamId);
        return ResponseEntity.ok(ApiResponse.success(workflows));
    }

    @GetMapping("/app/{appId}")
    public ResponseEntity<ApiResponse<List<WorkflowDTO>>> getWorkflowsByAppId(@PathVariable String appId) {
        log.info("Getting all workflows for app: {}", appId);
        List<WorkflowDTO> workflows = workflowService.getWorkflowsByAppId(appId);
        return ResponseEntity.ok(ApiResponse.success(workflows));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<WorkflowDTO>> updateWorkflow(@Valid @RequestBody WorkflowUpdateRequest request) {
        log.info("Updating workflow with ID: {}", request.getWorkflowId());
        WorkflowDTO updatedWorkflow = workflowService.updateWorkflow(request);
        return ResponseEntity.ok(ApiResponse.success(updatedWorkflow));
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(@PathVariable String workflowId) {
        log.info("Deleting workflow with ID: {}", workflowId);
        workflowService.deleteWorkflow(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Workflow deleted successfully", null));
    }

    @Operation(summary = "Execute workflow", description = "Execute a workflow with the given inputs")
    @PostMapping("/{workflowId}/execute")
    public ResponseEntity<Map<String, Object>> executeWorkflow(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, Object> inputs) {
        
        Map<String, Object> outputs = workflowService.executeWorkflow(workflowId, inputs != null ? inputs : new HashMap<>());
        return ResponseEntity.ok(outputs);
    }
    
    @Operation(summary = "Stream workflow execution", description = "Execute a workflow and stream results using Server-Sent Events")
    @PostMapping(value = "/{workflowId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWorkflow(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, Object> inputs) {
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        executorService.submit(() -> {
            try {
                // Set headers for SSE
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("Workflow execution started"));
                
                // Execute workflow with streaming
                workflowService.streamWorkflow(workflowId, 
                    inputs != null ? inputs : new HashMap<>(),
                    (chunk, isComplete) -> {
                        try {
                            if (chunk != null && !chunk.isEmpty()) {
                                emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(chunk));
                            }
                            
                            if (isComplete) {
                                emitter.send(SseEmitter.event()
                                    .name("end")
                                    .data("Workflow execution completed"));
                                emitter.complete();
                            }
                        } catch (IOException e) {
                            log.error("Error sending SSE event", e);
                            emitter.completeWithError(e);
                        }
                    });
            } catch (Exception e) {
                log.error("Error executing streaming workflow", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        // Send heartbeat to keep connection alive
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 300; i++) { // 5 minutes max (10s * 30)
                    Thread.sleep(10000); // Send heartbeat every 10 seconds
                    emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(""));
                }
            } catch (Exception e) {
                // Ignore, client likely disconnected
            }
        }, executorService);
        
        return emitter;
    }
    
    @Operation(summary = "Debug workflow execution", description = "Get debug information for a workflow execution")
    @GetMapping("/{workflowId}/debug/{executionId}")
    public ResponseEntity<WorkflowDebugResponse> getDebugInfo(
            @PathVariable String workflowId,
            @PathVariable String executionId) {
        
        WorkflowDebugResponse debugInfo = workflowService.getDebugInfo(workflowId, executionId);
        return ResponseEntity.ok(debugInfo);
    }
    
    @Operation(summary = "Debug node execution", description = "Execute a single node with the given inputs for debugging")
    @PostMapping("/{workflowId}/nodes/{nodeId}/debug")
    public ResponseEntity<NodeOutDTO> debugNode(
            @PathVariable String workflowId,
            @PathVariable String nodeId,
            @RequestBody(required = false) Map<String, Object> inputs) {
        
        NodeOutDTO result = workflowService.debugNode(workflowId, nodeId, inputs != null ? inputs : new HashMap<>());
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "Execute node", description = "Execute a single node with the given inputs")
    @PostMapping("/{workflowId}/nodes/{nodeId}/execute")
    public ResponseEntity<NodeOutDTO> executeNode(
            @PathVariable String workflowId,
            @PathVariable String nodeId,
            @RequestBody(required = false) Map<String, Object> inputs) {
        
        WorkflowDTO workflow = workflowService.getWorkflowById(workflowId);
        NodeOutDTO result = workflowService.executeNode(workflow, nodeId, inputs != null ? inputs : new HashMap<>());
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "Get workflow execution metadata", description = "Get execution metadata for a workflow")
    @GetMapping("/{workflowId}/metadata")
    public ResponseEntity<Map<String, Object>> getExecutionMetadata(@PathVariable String workflowId) {
        Map<String, Object> metadata = workflowService.getExecutionMetadata(workflowId);
        return ResponseEntity.ok(metadata);
    }
    
    @Operation(summary = "Submit interaction response", description = "Submit user response for an interactive workflow node")
    @PostMapping("/interaction/{executionId}/response")
    public ResponseEntity<Map<String, Object>> submitInteractionResponse(
            @PathVariable String executionId,
            @RequestBody Map<String, Object> userResponse) {
        
        if (!workflowInteractionService.isWaitingForInteraction(executionId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No pending interaction for this execution ID"
            ));
        }
        
        try {
            Map<String, Object> updatedContext = workflowInteractionService.resumeWithUserResponse(
                    executionId, userResponse.get("response"));
            
            // Resume workflow execution with the updated context
            return ResponseEntity.ok(workflowService.resumeExecution(executionId, updatedContext));
        } catch (Exception e) {
            log.error("Error processing interaction response", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to process interaction response: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "Get pending interaction", description = "Get information about a pending interactive node")
    @GetMapping("/interaction/{executionId}")
    public ResponseEntity<?> getPendingInteraction(@PathVariable String executionId) {
        return workflowInteractionService.getInteractionState(executionId)
                .map(interaction -> ResponseEntity.ok(interaction))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(summary = "Get user's pending interactions", description = "Get all pending interactions for a user")
    @GetMapping("/interactions/user/{userId}")
    public ResponseEntity<List<WorkflowInteractionState>> getUserPendingInteractions(@PathVariable String userId) {
        List<WorkflowInteractionState> interactions = workflowInteractionService.getUserPendingInteractions(userId);
        return ResponseEntity.ok(interactions);
    }
    
    @Operation(summary = "Cancel interaction", description = "Cancel a pending interaction")
    @DeleteMapping("/interaction/{executionId}")
    public ResponseEntity<Map<String, Object>> cancelInteraction(@PathVariable String executionId) {
        boolean cancelled = workflowInteractionService.cancelInteraction(executionId);
        
        if (cancelled) {
            return ResponseEntity.ok(Map.of("status", "cancelled"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Workflow Templates API
    
    @Operation(summary = "Get all templates", description = "Get all available workflow templates")
    @GetMapping("/templates")
    public ResponseEntity<List<WorkflowTemplateDTO>> getAllTemplates() {
        List<WorkflowTemplateDTO> templates = workflowTemplateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }
    
    @Operation(summary = "Get templates by category", description = "Get workflow templates by category")
    @GetMapping("/templates/category/{category}")
    public ResponseEntity<List<WorkflowTemplateDTO>> getTemplatesByCategory(@PathVariable String category) {
        List<WorkflowTemplateDTO> templates = workflowTemplateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(templates);
    }
    
    @Operation(summary = "Get template by ID", description = "Get a workflow template by ID")
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<WorkflowTemplateDTO> getTemplateById(@PathVariable String templateId) {
        try {
            WorkflowTemplateDTO template = workflowTemplateService.getTemplateById(templateId);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            log.error("Error getting template", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(summary = "Create workflow from template", description = "Instantiate a new workflow from a template")
    @PostMapping("/templates/{templateId}/instantiate")
    public ResponseEntity<WorkflowDTO> instantiateTemplate(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> parameters = request.containsKey("parameters") ? 
                (Map<String, Object>) request.get("parameters") : Map.of();
        String userId = (String) request.get("userId");
        String teamId = (String) request.get("teamId");
        String workflowName = (String) request.get("workflowName");
        
        try {
            WorkflowDTO workflow = workflowTemplateService.instantiateTemplate(
                    templateId, parameters, userId, teamId, workflowName);
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            log.error("Error instantiating template", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(summary = "Create template from workflow", description = "Create a new workflow template from an existing workflow")
    @PostMapping("/{workflowId}/create-template")
    public ResponseEntity<WorkflowTemplateDTO> createTemplateFromWorkflow(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> request) {
        
        String category = (String) request.get("category");
        String templateName = (String) request.get("templateName");
        String description = (String) request.get("description");
        Map<String, Object> parameterDefinitions = request.containsKey("parameterDefinitions") ?
                (Map<String, Object>) request.get("parameterDefinitions") : Map.of();
        
        try {
            WorkflowTemplateDTO template = workflowTemplateService.createTemplateFromWorkflow(
                    workflowId, category, templateName, description, parameterDefinitions);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            log.error("Error creating template", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Error in workflow controller", e);
        Map<String, String> response = new HashMap<>();
        response.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 