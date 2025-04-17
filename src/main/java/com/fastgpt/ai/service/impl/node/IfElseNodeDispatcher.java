package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 条件判断节点调度器
 * 对应Next.js版本的ifElseNode节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IfElseNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.IF_ELSE_NODE.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing if-else node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            @SuppressWarnings("unchecked")
            Map<String, Object> conditions = inputs.containsKey("conditions") && 
                inputs.get("conditions") instanceof Map ?
                (Map<String, Object>) inputs.get("conditions") : new HashMap<>();
            
            // 执行条件判断
            boolean result = evaluateConditions(conditions, inputs);
            log.debug("Condition evaluation result: {}", result);
            
            // 确定跳过的句柄
            List<String> skipHandleIds = new ArrayList<>();
            
            // 判断结果为假时，跳过true分支
            if (!result) {
                skipHandleIds.add("true");
            } else {
                // 判断结果为真时，跳过false分支
                skipHandleIds.add("false");
            }
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", result);
            outputs.put("skipHandleId", skipHandleIds);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("conditions", conditions);
            responseData.put("result", result);
            responseData.put("takenBranch", result ? "true" : "false");
            responseData.put("skipBranch", result ? "false" : "true");
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing if-else node: {}", node.getNodeId(), e);
            
            // 构建错误输出 - 默认跳过true分支
            List<String> skipHandleIds = Collections.singletonList("true");
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", false);
            outputs.put("skipHandleId", skipHandleIds);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 评估条件
     * 
     * @param conditions 条件配置
     * @param inputs 节点输入
     * @return 条件评估结果
     */
    private boolean evaluateConditions(Map<String, Object> conditions, Map<String, Object> inputs) {
        // 获取条件类型和参数
        String type = (String) conditions.getOrDefault("type", "");
        
        if ("logic".equalsIgnoreCase(type)) {
            return evaluateLogicCondition(conditions, inputs);
        } else if ("compare".equalsIgnoreCase(type)) {
            return evaluateCompareCondition(conditions, inputs);
        } else if ("text".equalsIgnoreCase(type)) {
            return evaluateTextCondition(conditions, inputs);
        } else if ("list".equalsIgnoreCase(type)) {
            return evaluateListCondition(conditions, inputs);
        } else {
            log.warn("Unknown condition type: {}", type);
            return false;
        }
    }
    
    /**
     * 评估逻辑条件
     */
    private boolean evaluateLogicCondition(Map<String, Object> conditions, Map<String, Object> inputs) {
        String operator = (String) conditions.getOrDefault("operator", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = conditions.containsKey("children") && 
            conditions.get("children") instanceof List ?
            (List<Map<String, Object>>) conditions.get("children") : new ArrayList<>();
        
        if (children.isEmpty()) {
            return false;
        }
        
        if ("and".equalsIgnoreCase(operator)) {
            // 所有条件都为真时结果为真
            for (Map<String, Object> child : children) {
                if (!evaluateConditions(child, inputs)) {
                    return false;
                }
            }
            return true;
        } else if ("or".equalsIgnoreCase(operator)) {
            // 任一条件为真时结果为真
            for (Map<String, Object> child : children) {
                if (evaluateConditions(child, inputs)) {
                    return true;
                }
            }
            return false;
        } else {
            log.warn("Unknown logic operator: {}", operator);
            return false;
        }
    }
    
    /**
     * 评估比较条件
     */
    private boolean evaluateCompareCondition(Map<String, Object> conditions, Map<String, Object> inputs) {
        String operator = (String) conditions.getOrDefault("operator", "");
        Object leftValue = getValueFromPath(conditions.get("left"), inputs);
        Object rightValue = getValueFromPath(conditions.get("right"), inputs);
        
        // 尝试进行数值比较
        if (leftValue instanceof Number && rightValue instanceof Number) {
            double leftNumber = ((Number) leftValue).doubleValue();
            double rightNumber = ((Number) rightValue).doubleValue();
            
            switch (operator) {
                case "==":
                    return Math.abs(leftNumber - rightNumber) < 0.00001;
                case "!=":
                    return Math.abs(leftNumber - rightNumber) >= 0.00001;
                case ">":
                    return leftNumber > rightNumber;
                case ">=":
                    return leftNumber >= rightNumber;
                case "<":
                    return leftNumber < rightNumber;
                case "<=":
                    return leftNumber <= rightNumber;
                default:
                    log.warn("Unknown compare operator: {}", operator);
                    return false;
            }
        } 
        // 进行字符串比较
        else {
            String leftStr = leftValue != null ? leftValue.toString() : "";
            String rightStr = rightValue != null ? rightValue.toString() : "";
            
            switch (operator) {
                case "==":
                    return leftStr.equals(rightStr);
                case "!=":
                    return !leftStr.equals(rightStr);
                case ">":
                    return leftStr.compareTo(rightStr) > 0;
                case ">=":
                    return leftStr.compareTo(rightStr) >= 0;
                case "<":
                    return leftStr.compareTo(rightStr) < 0;
                case "<=":
                    return leftStr.compareTo(rightStr) <= 0;
                default:
                    log.warn("Unknown compare operator: {}", operator);
                    return false;
            }
        }
    }
    
    /**
     * 评估文本条件
     */
    private boolean evaluateTextCondition(Map<String, Object> conditions, Map<String, Object> inputs) {
        String operator = (String) conditions.getOrDefault("operator", "");
        String text = getValueFromPath(conditions.get("text"), inputs).toString();
        String pattern = getValueFromPath(conditions.get("pattern"), inputs).toString();
        
        switch (operator) {
            case "includes":
                return text.contains(pattern);
            case "notIncludes":
                return !text.contains(pattern);
            case "startsWith":
                return text.startsWith(pattern);
            case "endsWith":
                return text.endsWith(pattern);
            case "matches":
                return text.matches(pattern);
            default:
                log.warn("Unknown text operator: {}", operator);
                return false;
        }
    }
    
    /**
     * 评估列表条件
     */
    private boolean evaluateListCondition(Map<String, Object> conditions, Map<String, Object> inputs) {
        String operator = (String) conditions.getOrDefault("operator", "");
        Object listObj = getValueFromPath(conditions.get("list"), inputs);
        Object item = getValueFromPath(conditions.get("item"), inputs);
        
        if (!(listObj instanceof List)) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) listObj;
        
        switch (operator) {
            case "includes":
                return list.contains(item);
            case "notIncludes":
                return !list.contains(item);
            default:
                log.warn("Unknown list operator: {}", operator);
                return false;
        }
    }
    
    /**
     * 从输入中获取值
     */
    private Object getValueFromPath(Object pathOrValue, Map<String, Object> inputs) {
        if (pathOrValue == null) {
            return "";
        }
        
        if (!(pathOrValue instanceof String)) {
            return pathOrValue;
        }
        
        String path = (String) pathOrValue;
        
        // 检查是否是输入引用
        if (path.startsWith("{{") && path.endsWith("}}")) {
            String key = path.substring(2, path.length() - 2).trim();
            return inputs.getOrDefault(key, "");
        }
        
        return path;
    }
} 