package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.WorkflowInteractionState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing workflow interactions
 */
public interface WorkflowInteractionService {
    
    /**
     * Create a new interaction state
     * @param workflowId Workflow ID
     * @param executionId Execution ID
     * @param nodeId Current node ID
     * @param interactionType Type of interaction
     * @param prompt Prompt for the user
     * @param options Options for selection
     * @param validationRules Validation rules
     * @param defaultValue Default value
     * @param context Current execution context
     * @return Created interaction state
     */
    WorkflowInteractionState createInteractionState(
            String workflowId,
            String executionId,
            String nodeId,
            WorkflowInteractionState.InteractionTypeEnum interactionType,
            String prompt,
            Map<String, Object> options,
            Map<String, Object> validationRules,
            Object defaultValue,
            Map<String, Object> context);
    
    /**
     * Get interaction state by interaction ID
     * @param interactionId Interaction ID
     * @return Optional containing the interaction state if found
     */
    Optional<WorkflowInteractionState> getInteractionState(String interactionId);
    
    /**
     * Get interaction state by execution ID
     * @param executionId Execution ID
     * @return Optional containing the interaction state if found
     */
    Optional<WorkflowInteractionState> getInteractionStateByExecutionId(String executionId);
    
    /**
     * Update interaction state to mark as processed
     * @param interactionId Interaction ID
     * @param context Updated context
     * @return Updated interaction state
     */
    WorkflowInteractionState completeInteraction(String interactionId, Map<String, Object> context);
    
    /**
     * List all pending interactions for a workflow
     * @param workflowId Workflow ID
     * @return List of pending interaction states
     */
    List<WorkflowInteractionState> getPendingInteractionsForWorkflow(String workflowId);
    
    /**
     * List all pending interactions for a user
     * @param userId User ID
     * @return List of pending interaction states
     */
    List<WorkflowInteractionState> getPendingInteractionsForUser(String userId);
    
    /**
     * Delete an interaction state
     * @param interactionId Interaction ID
     */
    void deleteInteractionState(String interactionId);
    
    /**
     * Clean up expired interaction states
     * @return Number of expired states removed
     */
    int cleanupExpiredInteractions();
} 