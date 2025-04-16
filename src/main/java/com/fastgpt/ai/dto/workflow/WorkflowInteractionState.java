package com.fastgpt.ai.dto.workflow;

import com.fastgpt.ai.constant.InteractionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents the state of a workflow execution that is waiting for user interaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInteractionState {
    /**
     * ID of the workflow execution
     */
    private String workflowExecutionId;
    
    /**
     * ID of the workflow
     */
    private String workflowId;
    
    /**
     * ID of the node requiring interaction
     */
    private String currentNodeId;
    
    /**
     * Type of interaction required
     */
    private InteractionTypeEnum interactionType;
    
    /**
     * Data related to the interaction (e.g., form fields, options)
     */
    private Object interactionData;
    
    /**
     * The current execution context variables
     */
    private Map<String, Object> currentContext;
    
    /**
     * Timestamp when the interaction was requested
     */
    private long requestedAt;
    
    /**
     * Whether the interaction has a timeout
     */
    private boolean hasTimeout;
    
    /**
     * Timeout duration in milliseconds (if hasTimeout is true)
     */
    private Long timeoutMs;
    
    /**
     * Optional message to display to the user
     */
    private String userMessage;
    
    /**
     * Optional title for the interaction
     */
    private String title;
    
    /**
     * Information about the next node(s) to execute after this interaction
     */
    private Map<String, Object> nextNodes;
    
    /**
     * Any additional metadata specific to this interaction
     */
    private Map<String, Object> metadata;
} 