package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.request.WorkflowCreateRequest;
import com.fastgpt.ai.dto.request.WorkflowUpdateRequest;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Service for workflow management and execution
 */
public interface WorkflowService {
    
    /**
     * Create a new workflow
     * @param request Workflow creation request
     * @return The created workflow DTO
     */
    WorkflowDTO createWorkflow(WorkflowCreateRequest request);
    
    /**
     * Get a workflow by ID
     * @param workflowId Workflow ID
     * @return Workflow DTO
     */
    WorkflowDTO getWorkflowById(String workflowId);
    
    /**
     * Get all workflows by user ID
     * @param userId User ID
     * @return List of workflow DTOs
     */
    List<WorkflowDTO> getWorkflowsByUserId(String userId);
    
    /**
     * Get all workflows by team ID
     * @param teamId Team ID
     * @return List of workflow DTOs
     */
    List<WorkflowDTO> getWorkflowsByTeamId(String teamId);
    
    /**
     * Get all workflows by app ID
     * @param appId App ID
     * @return List of workflow DTOs
     */
    List<WorkflowDTO> getWorkflowsByAppId(String appId);
    
    /**
     * Get all workflows by module ID
     * @param moduleId Module ID
     * @return List of workflow DTOs
     */
    List<WorkflowDTO> getWorkflowsByModuleId(String moduleId);
    
    /**
     * Get all workflow templates
     * @return List of workflow template DTOs
     */
    List<WorkflowDTO> getWorkflowTemplates();
    
    /**
     * Update a workflow
     * @param request Workflow update request
     * @return The updated workflow DTO
     */
    WorkflowDTO updateWorkflow(WorkflowUpdateRequest request);
    
    /**
     * Delete a workflow by ID
     * @param workflowId Workflow ID
     */
    void deleteWorkflow(String workflowId);
    
    /**
     * Execute a complete workflow with given inputs
     * @param workflowId Workflow ID
     * @param inputs Map of input values
     * @return Map of output values
     */
    Map<String, Object> executeWorkflow(String workflowId, Map<String, Object> inputs);
    
    /**
     * Dispatch a workflow execution, handling node sequencing and data flow
     * @param workflow Workflow DTO
     * @param inputs Map of input values
     * @param nodeId ID of the node to start execution from (optional, null means start from entry nodes)
     * @return Map of output values
     */
    Map<String, Object> dispatchWorkflow(WorkflowDTO workflow, Map<String, Object> inputs, String nodeId);
    
    /**
     * Execute a single node in a workflow
     * @param workflow Workflow DTO
     * @param nodeId Node ID
     * @param inputs Map of input values
     * @return NodeOutDTO containing execution results and outputs
     */
    NodeOutDTO executeNode(WorkflowDTO workflow, String nodeId, Map<String, Object> inputs);
    
    /**
     * Get workflows by user ID or team ID
     * @param userId User ID
     * @param teamId Team ID
     * @return List of workflow DTOs
     */
    List<WorkflowDTO> getWorkflowsByUserIdOrTeamId(String userId, String teamId);
    
    /**
     * Execute a workflow with streaming output
     * @param workflowId Workflow ID
     * @param inputs Input parameters
     * @param chunkConsumer Callback to receive text chunks, along with a flag indicating if it's the last chunk
     */
    void streamWorkflow(String workflowId, Map<String, Object> inputs, BiConsumer<String, Boolean> chunkConsumer);
    
    /**
     * Get metadata from a workflow execution
     * @param workflowId Workflow ID
     * @return Map containing execution metadata
     */
    Map<String, Object> getExecutionMetadata(String workflowId);
} 