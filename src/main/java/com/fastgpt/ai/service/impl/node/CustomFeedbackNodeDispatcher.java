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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 自定义反馈节点调度器
 * 对应Next.js版本的customFeedback节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomFeedbackNodeDispatcher implements NodeDispatcher {

    private final WorkflowInteractionService interactionService;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.CUSTOM_FEEDBACK.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing custom feedback node: {}", node.getNodeId());
        
        try {
            // 获取配置
            Map<String, Object> config = getConfigFromNode(node);
            
            // 提取参数
            String message = getStringValue(config, "message", "请提供反馈");
            String title = getStringValue(config, "title", "反馈请求");
            String type = getStringValue(config, "type", "text");
            Integer timeoutSeconds = getIntegerValue(config, "timeout", 300); // 默认5分钟超时
            
            // 创建交互上下文
            Map<String, Object> interactionContext = new HashMap<>(inputs);
            
            // 检查是否已经有用户回应
            if (Boolean.TRUE.equals(inputs.get("__resuming_from_interaction"))) {
                // 用户已经响应，继续处理
                Object feedbackValue = inputs.get("userResponse");
                
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("feedbackValue", feedbackValue);
                outputs.put("interactionCompleted", true);
                
                return NodeOutDTO.builder()
                        .success(true)
                        .nodeId(node.getNodeId())
                        .output(outputs)
                        .build();
            }
            
            // 获取或生成执行ID
            String workflowId = (String) inputs.getOrDefault("__workflow_id", UUID.randomUUID().toString());
            String executionId = (String) inputs.getOrDefault("__execution_id", "feedback-" + System.currentTimeMillis());
            
            // 构建交互数据
            Map<String, Object> interactionData = new HashMap<>();
            interactionData.put("message", message);
            interactionData.put("type", type);
            
            // 创建交互状态
            WorkflowInteractionState state = interactionService.createInteractionState(
                workflowId,
                executionId,
                node.getNodeId(),
                WorkflowInteractionState.InteractionTypeEnum.FEEDBACK,
                message,
                interactionData,
                new HashMap<>(), // 验证规则
                null, // 默认值
                interactionContext
            );
            
            // 构建暂停结果
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("__waiting_for_interaction", true);
            outputs.put("__interaction_type", "FEEDBACK");
            outputs.put("__interaction_message", message);
            
            return NodeOutDTO.builder()
                .success(true)
                .nodeId(node.getNodeId())
                .suspended(true)
                .interactionId(state.getInteractionId())
                .output(outputs)
                .build();
            
        } catch (Exception e) {
            log.error("Error in custom feedback node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("自定义反馈处理失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 从节点中获取配置
     */
    private Map<String, Object> getConfigFromNode(Node node) {
        // 根据实际节点结构获取配置
        // 优先从输入参数获取，如果没有则使用节点的其他属性
        try {
            // 尝试获取节点的输入列表中的配置
            for (var input : node.getInputs()) {
                if ("message".equals(input.getKey()) || 
                    "title".equals(input.getKey()) || 
                    "type".equals(input.getKey()) || 
                    "timeout".equals(input.getKey())) {
                    
                    Map<String, Object> config = new HashMap<>();
                    
                    for (var nodeInput : node.getInputs()) {
                        config.put(nodeInput.getKey(), nodeInput.getValue());
                    }
                    
                    return config;
                }
            }
            
            // 如果找不到输入参数，则尝试从其他属性获取
            return new HashMap<>();
        } catch (Exception e) {
            log.warn("Error extracting config from node: {}", e.getMessage());
            return new HashMap<>();
        }
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
    
    /**
     * 获取整数值
     */
    private Integer getIntegerValue(Map<String, Object> map, String key, Integer defaultValue) {
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
} 