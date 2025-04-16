package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.WorkflowInteractionState;
import com.fastgpt.ai.dto.WorkflowInteractionState.InteractionTypeEnum;
import com.fastgpt.ai.repository.WorkflowInteractionRepository;
import com.fastgpt.ai.service.WorkflowInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the WorkflowInteractionService
 * Note: This implementation uses an in-memory store for simplicity.
 * In a production environment, you would use a proper database via Repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowInteractionServiceImpl implements WorkflowInteractionService {
    
    // In-memory store of interaction states by ID
    private final Map<String, WorkflowInteractionState> interactionStates = new ConcurrentHashMap<>();
    
    // Indexes for quick lookups
    private final Map<String, String> executionIdToInteractionId = new ConcurrentHashMap<>();
    private final Map<String, List<String>> workflowIdToInteractionIds = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userIdToInteractionIds = new ConcurrentHashMap<>();
    
    // Optional repository for persistent storage
    private final Optional<WorkflowInteractionRepository> repository;
    
    @Override
    public WorkflowInteractionState createInteractionState(
            String workflowId,
            String executionId,
            String nodeId,
            InteractionTypeEnum interactionType,
            String prompt,
            Map<String, Object> options,
            Map<String, Object> validationRules,
            Object defaultValue,
            Map<String, Object> context) {
        
        log.info("Creating interaction state for workflow: {}, execution: {}, node: {}", 
                workflowId, executionId, nodeId);
        
        // Generate a new interaction ID
        String interactionId = WorkflowInteractionState.generateInteractionId();
        
        // Create new interaction state
        WorkflowInteractionState state = WorkflowInteractionState.builder()
                .interactionId(interactionId)
                .executionId(executionId)
                .workflowId(workflowId)
                .currentNodeId(nodeId)
                .interactionType(interactionType)
                .prompt(prompt)
                .options(options != null ? options : new HashMap<>())
                .validationRules(validationRules != null ? validationRules : new HashMap<>())
                .defaultValue(defaultValue)
                .context(context != null ? context : new HashMap<>())
                .createdAt(LocalDateTime.now())
                .expiresAt(WorkflowInteractionState.generateDefaultExpiration())
                .processed(false)
                .build();
        
        // Store in memory
        interactionStates.put(interactionId, state);
        executionIdToInteractionId.put(executionId, interactionId);
        
        // Store in repository if available
        repository.ifPresent(repo -> repo.save(state));
        
        log.info("Created interaction state with ID: {}", interactionId);
        return state;
    }
    
    @Override
    public Optional<WorkflowInteractionState> getInteractionState(String interactionId) {
        // Try to get from memory first
        WorkflowInteractionState state = interactionStates.get(interactionId);
        
        // If not in memory, try to get from repository
        if (state == null && repository.isPresent()) {
            state = repository.get().findById(interactionId).orElse(null);
            
            // Cache in memory if found
            if (state != null) {
                interactionStates.put(interactionId, state);
                executionIdToInteractionId.put(state.getExecutionId(), interactionId);
            }
        }
        
        return Optional.ofNullable(state);
    }
    
    @Override
    public Optional<WorkflowInteractionState> getInteractionStateByExecutionId(String executionId) {
        // Get interaction ID from index
        String interactionId = executionIdToInteractionId.get(executionId);
        
        // If found in index, get the interaction state
        if (interactionId != null) {
            return getInteractionState(interactionId);
        }
        
        // If not found in memory, try to get from repository
        if (repository.isPresent()) {
            Optional<WorkflowInteractionState> state = repository.get().findByExecutionId(executionId);
            
            // Cache in memory if found
            state.ifPresent(s -> {
                interactionStates.put(s.getInteractionId(), s);
                executionIdToInteractionId.put(executionId, s.getInteractionId());
            });
            
            return state;
        }
        
        return Optional.empty();
    }
    
    @Override
    public WorkflowInteractionState completeInteraction(String interactionId, Map<String, Object> context) {
        log.info("Completing interaction: {}", interactionId);
        
        // Get the interaction state
        Optional<WorkflowInteractionState> stateOpt = getInteractionState(interactionId);
        
        if (!stateOpt.isPresent()) {
            log.warn("Interaction not found: {}", interactionId);
            return null;
        }
        
        WorkflowInteractionState state = stateOpt.get();
        
        // Update context if provided
        if (context != null && !context.isEmpty()) {
            // Merge with existing context
            Map<String, Object> updatedContext = new HashMap<>(state.getContext());
            updatedContext.putAll(context);
            state.setContext(updatedContext);
        }
        
        // Mark as processed
        state.setProcessed(true);
        
        // Save changes
        interactionStates.put(interactionId, state);
        
        // Save to repository if available
        repository.ifPresent(repo -> repo.save(state));
        
        log.info("Interaction completed: {}", interactionId);
        return state;
    }
    
    @Override
    public List<WorkflowInteractionState> getPendingInteractionsForWorkflow(String workflowId) {
        // For a production implementation, use a database query
        // For this in-memory implementation, we'll filter all interactions
        return interactionStates.values().stream()
                .filter(state -> workflowId.equals(state.getWorkflowId()))
                .filter(state -> !state.isProcessed())
                .filter(state -> state.getExpiresAt().isAfter(LocalDateTime.now()))
                .toList();
    }
    
    @Override
    public List<WorkflowInteractionState> getPendingInteractionsForUser(String userId) {
        // For a real implementation, you'd have a userId field in the WorkflowInteractionState
        // and would query the database directly
        // For this simplified version, we return an empty list
        return Collections.emptyList();
    }
    
    @Override
    public void deleteInteractionState(String interactionId) {
        log.info("Deleting interaction: {}", interactionId);
        
        // Get the interaction to remove related indices
        Optional<WorkflowInteractionState> state = getInteractionState(interactionId);
        
        if (state.isPresent()) {
            // Remove from indices
            executionIdToInteractionId.remove(state.get().getExecutionId());
            
            // Remove from memory
            interactionStates.remove(interactionId);
            
            // Remove from repository if available
            repository.ifPresent(repo -> repo.deleteById(interactionId));
        }
    }
    
    @Override
    public int cleanupExpiredInteractions() {
        log.info("Cleaning up expired interactions");
        
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        
        // Find and remove expired interactions
        List<String> expiredIds = interactionStates.values().stream()
                .filter(state -> state.getExpiresAt().isBefore(now))
                .map(WorkflowInteractionState::getInteractionId)
                .toList();
        
        for (String id : expiredIds) {
            deleteInteractionState(id);
            count++;
        }
        
        log.info("Cleaned up {} expired interactions", count);
        return count;
    }
} 