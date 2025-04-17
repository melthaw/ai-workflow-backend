package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.WorkflowInteractionState;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import com.fastgpt.ai.service.WorkflowInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户选择节点调度器
 * 对应Next.js版本的userSelect节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSelectNodeDispatcher implements NodeDispatcher {

    private final WorkflowInteractionService interactionService;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.USER_SELECT.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing user select node: {}", node.getNodeId());

        try {
            // 获取节点配置
            Map<String, Object> nodeConfig = new HashMap<>();
            if (node.getConfig() != null) {
                nodeConfig = node.getConfig().getProperties() != null ? 
                    node.getConfig().getProperties() : new HashMap<>();
            }
            
            // 提取配置
            String title = getString(nodeConfig, "title", "请选择选项");
            String description = getString(nodeConfig, "description", "从以下选项中选择一个");
            Integer timeoutSeconds = getInteger(nodeConfig, "timeout", 300); // 默认5分钟超时
            
            // 获取可选项
            List<Map<String, Object>> options = getListOfMaps(nodeConfig, "options");
            if (options == null || options.isEmpty()) {
                throw new IllegalArgumentException("未定义选择选项");
            }
            
            // 构建选项Map
            Map<String, Object> optionsMap = new HashMap<>();
            for (Map<String, Object> option : options) {
                String value = getString(option, "value", "");
                String label = getString(option, "label", value);
                if (!value.isEmpty()) {
                    optionsMap.put(value, label);
                }
            }
            
            // 创建交互上下文
            Map<String, Object> interactionContext = new HashMap<>(inputs);
            
            // 检查是否已经有用户回应
            if (Boolean.TRUE.equals(inputs.get("__resuming_from_interaction"))) {
                // 用户已经响应，继续处理
                Object selectedValue = inputs.get("userResponse");
                
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("selected", selectedValue);
                outputs.put("selectedValue", selectedValue);
                outputs.put("interactionCompleted", true);
                
                return NodeOutDTO.builder()
                        .success(true)
                        .nodeId(node.getNodeId())
                        .outputs(outputs)
                        .build();
            }
            
            // 获取或生成执行ID
            String workflowId = (String) inputs.getOrDefault("__workflow_id", UUID.randomUUID().toString());
            String executionId = (String) inputs.getOrDefault("__execution_id", "select-" + System.currentTimeMillis());
            
            // 创建交互状态
            WorkflowInteractionState state = interactionService.createInteractionState(
                workflowId,
                executionId,
                node.getNodeId(),
                WorkflowInteractionState.InteractionTypeEnum.SELECT,
                description,
                optionsMap,
                new HashMap<>(), // 验证规则
                null, // 默认值
                interactionContext
            );
            
            // 构建挂起结果
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("__waiting_for_interaction", true);
            outputs.put("__interaction_type", "SELECT");
            outputs.put("__interaction_prompt", description);
            outputs.put("__interaction_options", optionsMap);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("interactionType", "SELECT");
            metadata.put("timeoutMs", timeoutSeconds * 1000L);
            
            return NodeOutDTO.builder()
                .success(true)
                .nodeId(node.getNodeId())
                .suspended(true)
                .interactionId(state.getInteractionId())
                .outputs(outputs)
                .metadata(metadata)
                .build();
            
        } catch (Exception e) {
            log.error("Error processing user select node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("用户选择节点处理失败: " + e.getMessage())
                .build();
        }
    }
    
    // 辅助方法: 从Map中获取字符串值
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) instanceof String) {
            return (String) map.get(key);
        }
        return defaultValue;
    }
    
    // 辅助方法: 从Map中获取整数值
    private Integer getInteger(Map<String, Object> map, String key, Integer defaultValue) {
        if (map != null && map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }
    
    // 辅助方法: 从Map中获取Map列表
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListOfMaps(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) instanceof List) {
            List<?> list = (List<?>) map.get(key);
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Object item : list) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            
            return result;
        }
        return new ArrayList<>();
    }
} 