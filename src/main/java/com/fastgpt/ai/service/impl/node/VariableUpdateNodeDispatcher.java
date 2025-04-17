package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 变量更新节点调度器
 * 对应Next.js版本的variableUpdate节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VariableUpdateNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.VARIABLE_UPDATE.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing variable update node: {}", node.getNodeId());
        
        try {
            // 获取更新操作
            @SuppressWarnings("unchecked")
            Map<String, Object> updates = inputs.containsKey("updates") && 
                inputs.get("updates") instanceof Map ?
                (Map<String, Object>) inputs.get("updates") : new HashMap<>();
            
            // 获取变量值
            @SuppressWarnings("unchecked")
            Map<String, Object> values = inputs.containsKey("values") && 
                inputs.get("values") instanceof Map ?
                (Map<String, Object>) inputs.get("values") : new HashMap<>();
            
            // 计算新的变量值
            Map<String, Object> newVariables = new HashMap<>();
            
            // 处理每个变量更新
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String variableName = entry.getKey();
                
                // 如果更新值是引用，则获取引用的变量值
                Object value = entry.getValue();
                if (value instanceof String) {
                    String valueStr = (String) value;
                    if (valueStr.startsWith("{{") && valueStr.endsWith("}}")) {
                        String referencedVar = valueStr.substring(2, valueStr.length() - 2).trim();
                        value = inputs.getOrDefault(referencedVar, "");
                    }
                }
                
                // 更新变量值
                newVariables.put(variableName, value);
                
                // 可能的变量类型转换
                if (value instanceof String) {
                    String valueStr = (String) value;
                    
                    // 尝试转换为数字
                    try {
                        if (valueStr.contains(".")) {
                            newVariables.put(variableName, Double.parseDouble(valueStr));
                        } else {
                            newVariables.put(variableName, Integer.parseInt(valueStr));
                        }
                    } catch (NumberFormatException ignored) {
                        // 如果不是有效的数字，保持字符串
                    }
                    
                    // 尝试转换为布尔值
                    if ("true".equalsIgnoreCase(valueStr)) {
                        newVariables.put(variableName, Boolean.TRUE);
                    } else if ("false".equalsIgnoreCase(valueStr)) {
                        newVariables.put(variableName, Boolean.FALSE);
                    }
                }
            }
            
            // 响应信息
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("variablesUpdated", newVariables.keySet());
            responseData.put("updateCount", newVariables.size());
            
            // 输出结果
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("success", true);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .newVariables(newVariables)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing variable update node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("success", false);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 