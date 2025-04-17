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
 * 循环节点调度器
 * 对应Next.js版本的loop节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoopNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.LOOP.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing loop node: {}", node.getNodeId());
        
        try {
            // 获取循环类型和参数
            String loopType = (String) inputs.getOrDefault("loopType", "count");
            
            // 处理不同类型的循环
            boolean shouldContinue = false;
            Map<String, Object> loopContext = new HashMap<>();
            List<String> skipHandleIds = new ArrayList<>();
            Map<String, Object> newVariables = new HashMap<>();
            
            switch (loopType) {
                case "count":
                    // 计数循环
                    int currentCount = inputs.containsKey("currentCount") ? 
                        Integer.parseInt(String.valueOf(inputs.get("currentCount"))) : 0;
                    int maxCount = inputs.containsKey("maxCount") ? 
                        Integer.parseInt(String.valueOf(inputs.get("maxCount"))) : 10;
                    
                    // 检查是否继续循环
                    shouldContinue = currentCount < maxCount;
                    
                    // 更新循环上下文
                    loopContext.put("currentCount", currentCount);
                    loopContext.put("maxCount", maxCount);
                    
                    // 更新变量
                    newVariables.put("loopCurrentCount", currentCount);
                    newVariables.put("loopMaxCount", maxCount);
                    
                    // 如果继续循环，准备下一次迭代
                    if (shouldContinue) {
                        newVariables.put("currentCount", currentCount + 1);
                    }
                    break;
                    
                case "collection":
                    // 集合循环
                    @SuppressWarnings("unchecked")
                    List<Object> items = inputs.containsKey("items") && inputs.get("items") instanceof List ?
                        (List<Object>) inputs.get("items") : new ArrayList<>();
                    
                    int currentIndex = inputs.containsKey("currentIndex") ? 
                        Integer.parseInt(String.valueOf(inputs.get("currentIndex"))) : 0;
                    
                    // 检查是否继续循环
                    shouldContinue = currentIndex < items.size();
                    
                    // 更新循环上下文
                    loopContext.put("currentIndex", currentIndex);
                    loopContext.put("items", items);
                    loopContext.put("itemsCount", items.size());
                    
                    // 更新变量
                    newVariables.put("loopCurrentIndex", currentIndex);
                    newVariables.put("loopItemsCount", items.size());
                    
                    // 如果有项目，设置当前项
                    if (shouldContinue && !items.isEmpty()) {
                        Object currentItem = items.get(currentIndex);
                        newVariables.put("loopCurrentItem", currentItem);
                        
                        // 如果是Map，展开所有键值对作为变量
                        if (currentItem instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemMap = (Map<String, Object>) currentItem;
                            for (Map.Entry<String, Object> entry : itemMap.entrySet()) {
                                newVariables.put("loopItem_" + entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    
                    // 如果继续循环，准备下一次迭代
                    if (shouldContinue) {
                        newVariables.put("currentIndex", currentIndex + 1);
                    }
                    break;
                    
                case "condition":
                    // 条件循环
                    @SuppressWarnings("unchecked")
                    Map<String, Object> condition = inputs.containsKey("condition") && 
                        inputs.get("condition") instanceof Map ?
                        (Map<String, Object>) inputs.get("condition") : new HashMap<>();
                    
                    int iterations = inputs.containsKey("iterations") ? 
                        Integer.parseInt(String.valueOf(inputs.get("iterations"))) : 0;
                    int maxIterations = inputs.containsKey("maxIterations") ? 
                        Integer.parseInt(String.valueOf(inputs.get("maxIterations"))) : 100;
                    
                    // 检查是否超过最大迭代次数
                    boolean belowMaxIterations = iterations < maxIterations;
                    
                    // 尝试评估条件
                    boolean conditionMet = evaluateCondition(condition, inputs);
                    
                    // 需要同时满足条件并且在最大迭代次数内
                    shouldContinue = conditionMet && belowMaxIterations;
                    
                    // 更新循环上下文
                    loopContext.put("iterations", iterations);
                    loopContext.put("maxIterations", maxIterations);
                    loopContext.put("conditionMet", conditionMet);
                    
                    // 更新变量
                    newVariables.put("loopIterations", iterations);
                    newVariables.put("loopMaxIterations", maxIterations);
                    
                    // 如果继续循环，准备下一次迭代
                    if (shouldContinue) {
                        newVariables.put("iterations", iterations + 1);
                    }
                    break;
                    
                default:
                    log.warn("Unknown loop type: {}", loopType);
                    shouldContinue = false;
            }
            
            // 确定流程方向
            if (shouldContinue) {
                // 继续循环，跳过"exit"句柄
                skipHandleIds.add("exit");
            } else {
                // 退出循环，跳过"continue"句柄
                skipHandleIds.add("continue");
            }
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("continue", shouldContinue);
            outputs.put("loopType", loopType);
            outputs.put("loopContext", loopContext);
            outputs.put("skipHandleId", skipHandleIds);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("loopType", loopType);
            responseData.put("continue", shouldContinue);
            responseData.put("loopContext", loopContext);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .newVariables(newVariables)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing loop node: {}", node.getNodeId(), e);
            
            // 构建错误输出 - 默认退出循环
            List<String> skipHandleIds = Collections.singletonList("continue");
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("continue", false);
            outputs.put("skipHandleId", skipHandleIds);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 评估循环条件
     */
    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> inputs) {
        try {
            String operator = (String) condition.getOrDefault("operator", "");
            Object leftValue = getValue(condition.get("left"), inputs);
            Object rightValue = getValue(condition.get("right"), inputs);
            
            if (leftValue instanceof Number && rightValue instanceof Number) {
                double left = ((Number) leftValue).doubleValue();
                double right = ((Number) rightValue).doubleValue();
                
                switch (operator) {
                    case "==": return Math.abs(left - right) < 0.00001;
                    case "!=": return Math.abs(left - right) >= 0.00001;
                    case ">": return left > right;
                    case ">=": return left >= right;
                    case "<": return left < right;
                    case "<=": return left <= right;
                    default: return false;
                }
            } else {
                String left = leftValue != null ? leftValue.toString() : "";
                String right = rightValue != null ? rightValue.toString() : "";
                
                switch (operator) {
                    case "==": return left.equals(right);
                    case "!=": return !left.equals(right);
                    case ">": return left.compareTo(right) > 0;
                    case ">=": return left.compareTo(right) >= 0;
                    case "<": return left.compareTo(right) < 0;
                    case "<=": return left.compareTo(right) <= 0;
                    case "contains": return left.contains(right);
                    case "notContains": return !left.contains(right);
                    case "startsWith": return left.startsWith(right);
                    case "endsWith": return left.endsWith(right);
                    default: return false;
                }
            }
        } catch (Exception e) {
            log.warn("Error evaluating condition: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从输入中获取值，处理变量引用
     */
    private Object getValue(Object value, Map<String, Object> inputs) {
        if (value == null) {
            return null;
        }
        
        if (!(value instanceof String)) {
            return value;
        }
        
        String strValue = (String) value;
        
        // 处理变量引用
        if (strValue.startsWith("{{") && strValue.endsWith("}}")) {
            String varName = strValue.substring(2, strValue.length() - 2).trim();
            return inputs.getOrDefault(varName, "");
        }
        
        return strValue;
    }
} 