package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.request.WorkflowCreateRequest;
import com.fastgpt.ai.dto.request.WorkflowUpdateRequest;
import com.fastgpt.ai.dto.response.ApiResponse;
import com.fastgpt.ai.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

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

    @PostMapping("/{workflowId}/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeWorkflow(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> inputs) {
        log.info("Executing workflow with ID: {}", workflowId);
        Map<String, Object> result = workflowService.executeWorkflow(workflowId, inputs);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
} 