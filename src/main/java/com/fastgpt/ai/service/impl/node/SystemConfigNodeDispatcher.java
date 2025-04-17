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
 * 系统配置节点调度器
 * 对应Next.js版本的systemConfig节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.SYSTEM_CONFIG.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing system config node: {}", node.getNodeId());
        
        try {
            // 获取系统配置参数
            Map<String, Object> nodeConfig = getNodeConfig(node);
            
            // 提取特定配置参数
            Boolean questionGuide = getBooleanValue(nodeConfig, "questionGuide", false);
            Boolean tts = getBooleanValue(nodeConfig, "tts", false);
            Boolean whisper = getBooleanValue(nodeConfig, "whisper", false);
            String chatInputGuide = getStringValue(nodeConfig, "chatInputGuide", "");
            Boolean autoExecute = getBooleanValue(nodeConfig, "autoExecute", false);
            
            // 处理变量配置
            List<Map<String, Object>> variables = getListValue(nodeConfig, "variables", new ArrayList<>());
            
            // 构建系统配置结果
            Map<String, Object> systemConfig = new HashMap<>();
            systemConfig.put("questionGuide", questionGuide);
            systemConfig.put("tts", tts);
            systemConfig.put("whisper", whisper);
            systemConfig.put("chatInputGuide", chatInputGuide);
            systemConfig.put("autoExecute", autoExecute);
            
            // 处理配置变量，这些变量将被添加到工作流的上下文中
            Map<String, Object> configVars = new HashMap<>();
            if (variables != null && !variables.isEmpty()) {
                for (Map<String, Object> variable : variables) {
                    String key = getStringValue(variable, "key", "");
                    Object value = variable.get("value");
                    String type = getStringValue(variable, "type", "string");
                    
                    if (!key.isEmpty() && value != null) {
                        // 根据类型转换值
                        Object typedValue = convertValueByType(value, type);
                        configVars.put(key, typedValue);
                    }
                }
            }
            
            // 更新上下文变量
            Map<String, Object> result = new HashMap<>();
            result.put("systemConfig", systemConfig);
            
            // 使用successWithVariables静态方法
            return NodeOutDTO.successWithVariables(result, configVars);
            
        } catch (Exception e) {
            log.error("Error in system config node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("系统配置处理失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 从节点获取配置
     */
    private Map<String, Object> getNodeConfig(Node node) {
        Map<String, Object> config = new HashMap<>();
        
        for (var input : node.getInputs()) {
            config.put(input.getKey(), input.getValue());
        }
        
        return config;
    }
    
    /**
     * 根据类型转换值
     */
    private Object convertValueByType(Object value, String type) {
        if (value == null) return null;
        
        try {
            switch (type.toLowerCase()) {
                case "string":
                    return value.toString();
                case "number":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    } else if (value instanceof String) {
                        return Double.parseDouble((String) value);
                    }
                    break;
                case "boolean":
                    if (value instanceof Boolean) {
                        return value;
                    } else if (value instanceof String) {
                        return Boolean.parseBoolean((String) value);
                    }
                    break;
                case "object":
                    if (value instanceof Map) {
                        return value;
                    } else if (value instanceof String) {
                        // 尝试解析JSON字符串
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            return mapper.readValue((String) value, Map.class);
                        } catch (Exception e) {
                            log.warn("Failed to parse JSON string: {}", e.getMessage());
                        }
                    }
                    break;
                case "array":
                    if (value instanceof List) {
                        return value;
                    } else if (value instanceof String) {
                        // 尝试解析JSON数组
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            return mapper.readValue((String) value, List.class);
                        } catch (Exception e) {
                            log.warn("Failed to parse JSON array: {}", e.getMessage());
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            log.warn("Error converting value: {}, type: {}, error: {}", value, type, e.getMessage());
        }
        
        // 默认返回原始值
        return value;
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
     * 获取布尔值
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        if (map != null && map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取列表值
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getListValue(Map<String, Object> map, String key, List<T> defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) instanceof List) {
            return (List<T>) map.get(key);
        }
        return defaultValue;
    }
} 