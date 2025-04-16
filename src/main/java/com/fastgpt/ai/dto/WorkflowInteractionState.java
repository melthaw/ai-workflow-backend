package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the state of a workflow that is waiting for user interaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInteractionState {
    
    /**
     * Unique ID for this interaction
     */
    private String interactionId;
    
    /**
     * Workflow execution ID
     */
    private String executionId;
    
    /**
     * Workflow ID
     */
    private String workflowId;
    
    /**
     * Node ID where interaction is required
     */
    private String currentNodeId;
    
    /**
     * Type of interaction required
     */
    private InteractionTypeEnum interactionType;
    
    /**
     * Prompt or instructions for the user
     */
    private String prompt;
    
    /**
     * Options for selection (for select interaction types)
     */
    private Map<String, Object> options;
    
    /**
     * Validation rules for input
     */
    private Map<String, Object> validationRules;
    
    /**
     * Default value or selection
     */
    private Object defaultValue;
    
    /**
     * Current execution context
     */
    private Map<String, Object> context;
    
    /**
     * Time when interaction was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Time when interaction expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Whether this interaction has been processed
     */
    private boolean processed;
    
    /**
     * Generate a new interaction ID
     */
    public static String generateInteractionId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generate a default expiration time (1 hour from now)
     */
    public static LocalDateTime generateDefaultExpiration() {
        return LocalDateTime.now().plusHours(1);
    }
    
    /**
     * Interaction types
     */
    public enum InteractionTypeEnum {
        /**
         * Select one from multiple options
         */
        SELECT,
        
        /**
         * Select multiple from options
         */
        MULTISELECT,
        
        /**
         * Free text input
         */
        TEXT_INPUT,
        
        /**
         * Form with multiple inputs
         */
        FORM,
        
        /**
         * Upload a file
         */
        FILE_UPLOAD,
        
        /**
         * Confirmation dialog
         */
        CONFIRM,
        
        /**
         * Custom interaction type
         */
        CUSTOM
    }
} 