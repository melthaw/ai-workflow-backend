package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.dto.WorkflowInteractionState;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import com.fastgpt.ai.service.WorkflowInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 表单输入节点调度器
 * 对应Next.js版本的formInput节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormInputNodeDispatcher implements NodeDispatcher {

    private final WorkflowInteractionService interactionService;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.FORM_INPUT.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing form input node: {}", node.getNodeId());
        
        try {
            // 获取节点配置
            Map<String, Object> nodeConfig = node.getConfig() != null && node.getConfig().getProperties() != null 
                ? node.getConfig().getProperties() 
                : new HashMap<>();
            
            String title = getString(nodeConfig, "title", "表单输入");
            String description = getString(nodeConfig, "description", "请填写以下表单");
            Integer timeoutSeconds = getInteger(nodeConfig, "timeout", 300); // 默认5分钟超时
            
            // 获取表单字段定义
            List<Map<String, Object>> fields = getListOfMaps(nodeConfig, "fields");
            if (fields == null || fields.isEmpty()) {
                throw new IllegalArgumentException("表单字段未定义");
            }
            
            // 创建交互上下文
            Map<String, Object> interactionContext = new HashMap<>(inputs);
            
            // 准备交互数据
            Map<String, Object> options = new HashMap<>();
            options.put("fields", fields);
            
            // 获取或生成执行ID
            String workflowId = (String) inputs.getOrDefault("__workflow_id", UUID.randomUUID().toString());
            String executionId = (String) inputs.getOrDefault("__execution_id", "form-" + System.currentTimeMillis());
            
            // 检查是否已经有用户回应
            if (Boolean.TRUE.equals(inputs.get("__resuming_from_interaction"))) {
                // 用户已经响应，继续处理
                Map<String, Object> formData = inputs.get("userResponse") instanceof Map
                        ? (Map<String, Object>) inputs.get("userResponse")
                        : new HashMap<>();
                
                Map<String, Object> outputs = new HashMap<>(formData);
                outputs.put("formData", formData);
                outputs.put("interactionCompleted", true);
                
                return NodeOutDTO.builder()
                        .success(true)
                        .nodeId(node.getNodeId())
                        .outputs(outputs)
                        .build();
            }
            
            // 创建交互状态
            WorkflowInteractionState state = interactionService.createInteractionState(
                workflowId,
                executionId,
                node.getNodeId(),
                WorkflowInteractionState.InteractionTypeEnum.FORM,
                description,
                options,
                new HashMap<>(), // 验证规则
                new HashMap<>(), // 默认值
                interactionContext
            );
            
            // 创建输出
            NodeOutDTO result = NodeOutDTO.builder()
                .success(true)
                .nodeId(node.getNodeId())
                .suspended(true) // 标记为暂停，等待用户交互
                .interactionId(state.getInteractionId())
                .build();
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("__waiting_for_interaction", true);
            outputs.put("__interaction_type", "FORM");
            outputs.put("__interaction_prompt", description);
            outputs.put("__interaction_form_fields", fields);
            result.setOutputs(outputs);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("interactionType", "FORM");
            metadata.put("timeoutMs", timeoutSeconds * 1000L);
            result.setMetadata(metadata);
            
            log.info("Form input node {} awaiting user interaction", node.getNodeId());
            return result;
            
        } catch (Exception e) {
            log.error("Error processing form input node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("表单输入节点处理失败: " + e.getMessage())
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