package com.fastgpt.ai.repository;

import com.fastgpt.ai.dto.WorkflowInteractionState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for workflow interaction states
 */
@Repository
public interface WorkflowInteractionRepository extends MongoRepository<WorkflowInteractionState, String> {
    
    /**
     * Find interaction state by execution ID
     * @param executionId Execution ID
     * @return Optional containing the interaction state if found
     */
    Optional<WorkflowInteractionState> findByExecutionId(String executionId);
    
    /**
     * Find all interaction states for a workflow
     * @param workflowId Workflow ID
     * @return List of interaction states
     */
    List<WorkflowInteractionState> findByWorkflowId(String workflowId);
    
    /**
     * Find all non-processed interaction states for a workflow
     * @param workflowId Workflow ID
     * @param processed Processed flag
     * @return List of interaction states
     */
    List<WorkflowInteractionState> findByWorkflowIdAndProcessed(String workflowId, boolean processed);
    
    /**
     * Find all expired interaction states
     * @param expiresAt Expiration date before which to find interactions
     * @return List of expired interaction states
     */
    List<WorkflowInteractionState> findByExpiresAtBefore(LocalDateTime expiresAt);
    
    /**
     * Delete all expired interaction states
     * @param expiresAt Expiration date before which to delete interactions
     * @return Number of deleted interactions
     */
    long deleteByExpiresAtBefore(LocalDateTime expiresAt);
} 