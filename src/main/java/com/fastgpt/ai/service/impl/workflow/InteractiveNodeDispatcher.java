package com.fastgpt.ai.service.impl.workflow;

import com.fastgpt.ai.dto.WorkflowInteractionState;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.WorkflowInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dispatcher for interactive nodes that require user input
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InteractiveNodeDispatcher {
    
    private final WorkflowInteractionService interactionService;
    
    /**
     * Process a user selection node
     * @param node Node definition
     * @param inputs Node inputs
     * @return Node output with interaction request
     */
    public NodeOutDTO dispatchUserSelectNode(Node node, Map<String, Object> inputs) {
        return dispatchUserSelectNode(convertToNodeDefDTO(node), inputs);
    }
    
    /**
     * Process a user selection node
     * @param node Node definition
     * @param inputs Node inputs
     * @return Node output with interaction request
     */
    public NodeOutDTO dispatchUserSelectNode(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing user select node: {}", node.getId());
        
        try {
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Extract node configuration
            String prompt = (String) nodeData.getOrDefault("prompt", "Please select an option:");
            
            // Get options to display to user
            @SuppressWarnings("unchecked")
            Map<String, Object> options = (Map<String, Object>) nodeData.getOrDefault("options", new HashMap<>());
            
            // Get optional validation rules
            @SuppressWarnings("unchecked")
            Map<String, Object> validationRules = (Map<String, Object>) nodeData.getOrDefault("validationRules", new HashMap<>());
            
            // Get default value if any
            Object defaultValue = nodeData.get("defaultValue");
            
            // Check if resuming from interaction
            if (Boolean.TRUE.equals(inputs.get("__resuming_from_interaction"))) {
                // User has already responded, proceed with output
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("selected", inputs.get("userResponse"));
                outputs.put("interactionCompleted", true);
                
                return NodeOutDTO.success(outputs);
            } else {
                // Need to pause for user interaction
                String workflowId = (String) inputs.get("__workflow_id");
                String executionId = (String) inputs.get("__execution_id");
                
                if (workflowId == null || executionId == null) {
                    // Generate IDs if not provided
                    workflowId = workflowId != null ? workflowId : UUID.randomUUID().toString();
                    executionId = executionId != null ? executionId : UUID.randomUUID().toString();
                    inputs.put("__workflow_id", workflowId);
                    inputs.put("__execution_id", executionId);
                }
                
                // Create interaction state
                WorkflowInteractionState interactionState = interactionService.createInteractionState(
                        workflowId,
                        executionId,
                        node.getId(),
                        WorkflowInteractionState.InteractionTypeEnum.SELECT,
                        prompt,
                        options,
                        validationRules,
                        defaultValue,
                        inputs
                );
                
                // Return suspended status
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("__waiting_for_interaction", true);
                outputs.put("__interaction_type", "SELECT");
                outputs.put("__interaction_prompt", prompt);
                outputs.put("__interaction_options", options);
                
                return NodeOutDTO.builder()
                        .success(true)
                        .suspended(true)
                        .interactionId(interactionState.getInteractionId())
                        .outputs(outputs)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error processing user select node: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Error in user select node: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process a form input node
     * @param node Node definition
     * @param inputs Node inputs
     * @return Node output with interaction request
     */
    public NodeOutDTO dispatchFormInputNode(Node node, Map<String, Object> inputs) {
        return dispatchFormInputNode(convertToNodeDefDTO(node), inputs);
    }
    
    /**
     * Process a form input node
     * @param node Node definition
     * @param inputs Node inputs
     * @return Node output with interaction request
     */
    public NodeOutDTO dispatchFormInputNode(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing form input node: {}", node.getId());
        
        try {
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Extract node configuration
            String prompt = (String) nodeData.getOrDefault("prompt", "Please fill in the form:");
            
            // Get form fields
            @SuppressWarnings("unchecked")
            Map<String, Object> formFields = (Map<String, Object>) nodeData.getOrDefault("fields", new HashMap<>());
            
            // Get validation rules
            @SuppressWarnings("unchecked")
            Map<String, Object> validationRules = (Map<String, Object>) nodeData.getOrDefault("validationRules", new HashMap<>());
            
            // Get default values
            @SuppressWarnings("unchecked")
            Map<String, Object> defaultValues = (Map<String, Object>) nodeData.getOrDefault("defaultValues", new HashMap<>());
            
            // Check if resuming from interaction
            if (Boolean.TRUE.equals(inputs.get("__resuming_from_interaction"))) {
                // User has already responded, proceed with output
                @SuppressWarnings("unchecked")
                Map<String, Object> formData = inputs.get("userResponse") instanceof Map
                        ? (Map<String, Object>) inputs.get("userResponse")
                        : new HashMap<>();
                
                Map<String, Object> outputs = new HashMap<>(formData);
                outputs.put("formData", formData);
                outputs.put("interactionCompleted", true);
                
                return NodeOutDTO.success(outputs);
            } else {
                // Need to pause for user interaction
                String workflowId = (String) inputs.get("__workflow_id");
                String executionId = (String) inputs.get("__execution_id");
                
                if (workflowId == null || executionId == null) {
                    // Generate IDs if not provided
                    workflowId = workflowId != null ? workflowId : UUID.randomUUID().toString();
                    executionId = executionId != null ? executionId : UUID.randomUUID().toString();
                    inputs.put("__workflow_id", workflowId);
                    inputs.put("__execution_id", executionId);
                }
                
                // Build options map with form fields
                Map<String, Object> options = new HashMap<>();
                options.put("fields", formFields);
                
                // Create interaction state
                WorkflowInteractionState interactionState = interactionService.createInteractionState(
                        workflowId,
                        executionId,
                        node.getId(),
                        WorkflowInteractionState.InteractionTypeEnum.FORM,
                        prompt,
                        options,
                        validationRules,
                        defaultValues,
                        inputs
                );
                
                // Return suspended status
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("__waiting_for_interaction", true);
                outputs.put("__interaction_type", "FORM");
                outputs.put("__interaction_prompt", prompt);
                outputs.put("__interaction_form_fields", formFields);
                
                return NodeOutDTO.builder()
                        .success(true)
                        .suspended(true)
                        .interactionId(interactionState.getInteractionId())
                        .outputs(outputs)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error processing form input node: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Error in form input node: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to convert Node to NodeDefDTO
     */
    private NodeDefDTO convertToNodeDefDTO(Node node) {
        NodeDefDTO nodeDefDTO = new NodeDefDTO();
        nodeDefDTO.setId(node.getNodeId());
        nodeDefDTO.setType(node.getType());
        nodeDefDTO.setData(node.getData());
        return nodeDefDTO;
    }
} 