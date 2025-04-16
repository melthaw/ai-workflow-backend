package com.fastgpt.ai.service.impl.workflow;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatcher for if-else conditional branching node
 */
@Slf4j
@Component
public class IfElseDispatcher {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * Process an if-else node
     */
    public NodeOutDTO dispatchIfElse(Node node, Map<String, Object> inputs) {
        log.info("Processing if-else node: {}", node.getId());
        
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
        
        // Get the condition from node data
        String condition = getStringParam(nodeData, "condition", "");
        if (condition.isEmpty()) {
            return createErrorResult(node, "Condition is empty");
        }
        
        try {
            // Create evaluation context with all input variables
            StandardEvaluationContext context = new StandardEvaluationContext();
            inputs.forEach(context::setVariable);
            
            // Evaluate the condition
            boolean conditionResult = Boolean.TRUE.equals(
                expressionParser.parseExpression(condition).getValue(context, Boolean.class)
            );
            
            // Set the result in outputs
            outputs.put("conditionResult", conditionResult);
            
            // Add the condition itself for debugging
            outputs.put("condition", condition);
            
            // Success result
            NodeOutDTO result = new NodeOutDTO();
            result.setNodeId(node.getNodeId());
            result.setOutputs(outputs);
            result.setSuccess(true);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("branch", conditionResult ? "then" : "else");
            result.setMetadata(metadata);
            
            return result;
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", condition, e);
            return createErrorResult(node, "Error evaluating condition: " + e.getMessage());
        }
    }
    
    /**
     * Create an error result
     */
    private NodeOutDTO createErrorResult(Node node, String errorMessage) {
        NodeOutDTO result = new NodeOutDTO();
        result.setNodeId(node.getNodeId());
        result.setSuccess(false);
        result.setError(errorMessage);
        return result;
    }
    
    /**
     * Get string parameter with default value
     */
    private String getStringParam(Map<String, Object> data, String key, String defaultValue) {
        return data.containsKey(key) && data.get(key) instanceof String 
            ? (String) data.get(key) 
            : defaultValue;
    }
} 