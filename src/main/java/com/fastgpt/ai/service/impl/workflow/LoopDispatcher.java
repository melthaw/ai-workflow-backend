package com.fastgpt.ai.service.impl.workflow;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatcher for loop-related workflow nodes
 */
@Slf4j
@Component
public class LoopDispatcher {
    
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    /**
     * Process a loop node (controls iteration)
     */
    public NodeOutDTO dispatchLoop(Node node, Map<String, Object> inputs) {
        log.info("Processing loop node: {}", node.getId());
        
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> nodeData = node.getData();
        
        // Extract loop parameters
        String loopType = getStringParam(nodeData, "loopType", "forEach");
        Integer maxIterations = getIntParam(nodeData, "maxIterations", 100);
        Boolean shouldContinue = getBooleanParam(inputs, "shouldContinue", true);
        Integer currentIteration = getIntParam(inputs, "currentIteration", 0);
        
        // Increment iteration counter
        currentIteration++;
        
        // Check if we should continue looping
        if (currentIteration > maxIterations) {
            shouldContinue = false;
        }
        
        // Prepare outputs
        outputs.put("currentIteration", currentIteration);
        outputs.put("shouldContinue", shouldContinue);
        outputs.put("maxIterations", maxIterations);
        
        NodeOutDTO result = new NodeOutDTO();
        result.setNodeId(node.getNodeId());
        result.setOutputs(outputs);
        result.setSuccess(true);
        return result;
    }
    
    /**
     * Process a loop start node (initializes iteration)
     */
    public NodeOutDTO dispatchLoopStart(Node node, Map<String, Object> inputs) {
        log.info("Processing loop start node: {}", node.getId());
        
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> nodeData = node.getData();
        
        // Extract loop parameters
        String loopType = getStringParam(nodeData, "loopType", "forEach");
        String loopVariable = getStringParam(nodeData, "loopVariable", "item");
        String targetListKey = getStringParam(nodeData, "targetList", "items");
        Integer maxIterations = getIntParam(nodeData, "maxIterations", 100);
        
        // Reset iteration state
        outputs.put("currentIteration", 0);
        outputs.put("shouldContinue", true);
        outputs.put("maxIterations", maxIterations);
        outputs.put("loopType", loopType);
        outputs.put("loopVariable", loopVariable);
        
        // Handle forEach loops (iterate over a list)
        if ("forEach".equals(loopType) && inputs.containsKey(targetListKey)) {
            List<?> targetList = getListParam(inputs, targetListKey);
            outputs.put("targetList", targetList);
            outputs.put("totalItems", targetList.size());
            
            // Set first item if list is not empty
            if (!targetList.isEmpty()) {
                outputs.put(loopVariable, targetList.get(0));
                outputs.put("currentIndex", 0);
            } else {
                outputs.put("shouldContinue", false);
            }
        } 
        // Handle while loops (continue while condition is true)
        else if ("while".equals(loopType)) {
            String condition = getStringParam(nodeData, "condition", "true");
            boolean conditionResult = evaluateCondition(condition, inputs);
            outputs.put("shouldContinue", conditionResult);
        }
        
        NodeOutDTO result = new NodeOutDTO();
        result.setNodeId(node.getNodeId());
        result.setOutputs(outputs);
        result.setSuccess(true);
        return result;
    }
    
    /**
     * Process a loop end node (updates iteration state)
     */
    public NodeOutDTO dispatchLoopEnd(Node node, Map<String, Object> inputs) {
        log.info("Processing loop end node: {}", node.getId());
        
        Map<String, Object> outputs = new HashMap<>();
        
        // Extract loop state
        Integer currentIteration = getIntParam(inputs, "currentIteration", 0);
        Integer maxIterations = getIntParam(inputs, "maxIterations", 100);
        String loopType = getStringParam(inputs, "loopType", "forEach");
        String loopVariable = getStringParam(inputs, "loopVariable", "item");
        Boolean shouldContinue = getBooleanParam(inputs, "shouldContinue", false);
        
        // Pass through state
        outputs.put("currentIteration", currentIteration);
        outputs.put("maxIterations", maxIterations);
        outputs.put("loopType", loopType);
        outputs.put("loopVariable", loopVariable);
        
        // Handle forEach loops
        if ("forEach".equals(loopType) && inputs.containsKey("targetList")) {
            List<?> targetList = getListParam(inputs, "targetList");
            Integer currentIndex = getIntParam(inputs, "currentIndex", 0);
            
            // Increment index for next iteration
            currentIndex++;
            
            // Check if we have more items
            if (currentIndex < targetList.size() && currentIteration < maxIterations) {
                outputs.put("currentIndex", currentIndex);
                outputs.put(loopVariable, targetList.get(currentIndex));
                outputs.put("shouldContinue", true);
            } else {
                outputs.put("shouldContinue", false);
            }
        } 
        // Handle while loops
        else if ("while".equals(loopType) && inputs.containsKey("condition")) {
            String condition = getStringParam(inputs, "condition", "true");
            
            // Only evaluate if we haven't exceeded max iterations
            if (currentIteration < maxIterations) {
                boolean conditionResult = evaluateCondition(condition, inputs);
                outputs.put("shouldContinue", conditionResult);
            } else {
                outputs.put("shouldContinue", false);
            }
        } else {
            outputs.put("shouldContinue", false);
        }
        
        NodeOutDTO result = new NodeOutDTO();
        result.setNodeId(node.getNodeId());
        result.setOutputs(outputs);
        result.setSuccess(true);
        return result;
    }
    
    /**
     * Evaluate a condition expression against input variables
     */
    private boolean evaluateCondition(String condition, Map<String, Object> inputs) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            
            // Add all inputs as variables in the expression context
            inputs.forEach(context::setVariable);
            
            // Evaluate the condition
            return Boolean.TRUE.equals(
                expressionParser.parseExpression(condition).getValue(context, Boolean.class)
            );
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", condition, e);
            return false;
        }
    }
    
    // Helper methods for parameter extraction
    
    private String getStringParam(Map<String, Object> data, String key, String defaultValue) {
        return data.containsKey(key) && data.get(key) instanceof String 
            ? (String) data.get(key) 
            : defaultValue;
    }
    
    private Integer getIntParam(Map<String, Object> data, String key, Integer defaultValue) {
        if (data.containsKey(key)) {
            if (data.get(key) instanceof Integer) {
                return (Integer) data.get(key);
            } else if (data.get(key) instanceof Number) {
                return ((Number) data.get(key)).intValue();
            } else if (data.get(key) instanceof String) {
                try {
                    return Integer.parseInt((String) data.get(key));
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }
    
    private Boolean getBooleanParam(Map<String, Object> data, String key, Boolean defaultValue) {
        return data.containsKey(key) && data.get(key) instanceof Boolean 
            ? (Boolean) data.get(key) 
            : defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private List<?> getListParam(Map<String, Object> data, String key) {
        if (data.containsKey(key)) {
            if (data.get(key) instanceof List) {
                return (List<?>) data.get(key);
            }
        }
        return new ArrayList<>();
    }
} 