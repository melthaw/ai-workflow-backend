package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 工具参数节点调度器
 * 对应Next.js版本的toolParams节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolParamsNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.TOOL_PARAMS.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing tool params node: {}", node.getNodeId());
        
        try {
            // 获取工具参数配置
            Map<String, Object> toolParams = extractToolParams(node, inputs);
            
            // 获取目标工具ID
            String toolId = getStringValue(toolParams, "toolId", "");
            String toolName = getStringValue(toolParams, "toolName", "未命名工具");
            
            if (toolId.isEmpty()) {
                return NodeOutDTO.builder()
                    .success(false)
                    .nodeId(node.getNodeId())
                    .error("未指定工具ID")
                    .build();
            }
            
            // 收集工具参数
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<String, Object> entry : toolParams.entrySet()) {
                if (!entry.getKey().equals("toolId") && !entry.getKey().equals("toolName")) {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
            
            log.info("Tool params for tool {}: {}", toolId, params);
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("toolId", toolId);
            result.put("toolName", toolName);
            result.put("params", params);
            
            return NodeOutDTO.builder()
                .success(true)
                .nodeId(node.getNodeId())
                .output(result)
                .build();
            
        } catch (Exception e) {
            log.error("Error in tool params node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("处理工具参数失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 提取工具参数
     */
    private Map<String, Object> extractToolParams(Node node, Map<String, Object> inputs) {
        Map<String, Object> params = new HashMap<>();
        
        // 从节点输入中获取参数
        for (var input : node.getInputs()) {
            // 获取参数值，可能是静态配置或动态输入
            String key = input.getKey();
            Object value = input.getValue();
            
            // 检查是否有来自上下文的动态输入
            if (inputs.containsKey(key)) {
                value = inputs.get(key);
            }
            
            if (value != null) {
                params.put(key, value);
            }
        }
        
        // 处理特殊的动态输入参数
        if (node.getInputs().stream().anyMatch(input -> "system_addInputParam".equals(input.getKey()))) {
            Object dynamicParams = inputs.get("system_addInputParam");
            if (dynamicParams instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dynamicMap = (Map<String, Object>) dynamicParams;
                params.putAll(dynamicMap);
            }
        }
        
        return params;
    }
    
    /**
     * 获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof String) {
                return (String) value;
            } else if (value != null) {
                return String.valueOf(value);
            }
        }
        return defaultValue;
    }
} 