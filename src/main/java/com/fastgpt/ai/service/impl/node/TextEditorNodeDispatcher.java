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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 文本编辑器节点调度器
 * 对应Next.js版本的textEditor节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextEditorNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.TEXT_EDITOR.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing text editor node: {}", node.getNodeId());
        
        try {
            // 获取输入文本
            String text = getStringValue(inputs, "text", "");
            if (text.isEmpty()) {
                return NodeOutDTO.builder()
                    .success(false)
                    .nodeId(node.getNodeId())
                    .error("没有输入文本")
                    .build();
            }
            
            // 获取编辑操作配置
            Map<String, Object> nodeConfig = new HashMap<>();
            for (var input : node.getInputs()) {
                nodeConfig.put(input.getKey(), input.getValue());
            }
            
            // 应用文本编辑操作
            String processedText = text;
            
            // 1. 去除前后空白
            if (getBooleanValue(nodeConfig, "trim", false)) {
                processedText = processedText.trim();
            }
            
            // 2. 大小写转换
            String caseOperation = getStringValue(nodeConfig, "case", "");
            if (!caseOperation.isEmpty()) {
                switch (caseOperation.toLowerCase()) {
                    case "upper":
                        processedText = processedText.toUpperCase();
                        break;
                    case "lower":
                        processedText = processedText.toLowerCase();
                        break;
                    case "title":
                        processedText = toTitleCase(processedText);
                        break;
                }
            }
            
            // 3. 替换操作
            List<Map<String, Object>> replaceOperations = getListValue(nodeConfig, "replace");
            if (replaceOperations != null) {
                for (Map<String, Object> replace : replaceOperations) {
                    String pattern = getStringValue(replace, "pattern", "");
                    String replacement = getStringValue(replace, "replacement", "");
                    
                    if (!pattern.isEmpty()) {
                        processedText = processedText.replace(pattern, replacement);
                    }
                }
            }
            
            // 4. 添加前缀
            String prefix = getStringValue(nodeConfig, "prefix", "");
            if (!prefix.isEmpty()) {
                processedText = prefix + processedText;
            }
            
            // 5. 添加后缀
            String suffix = getStringValue(nodeConfig, "suffix", "");
            if (!suffix.isEmpty()) {
                processedText = processedText + suffix;
            }
            
            // 6. 分割和连接
            String delimiter = getStringValue(nodeConfig, "splitDelimiter", "");
            String joinDelimiter = getStringValue(nodeConfig, "joinDelimiter", "");
            if (!delimiter.isEmpty() && !joinDelimiter.isEmpty()) {
                String[] parts = processedText.split(Pattern.quote(delimiter));
                processedText = String.join(joinDelimiter, parts);
            }
            
            // 7. 正则表达式替换
            String regexPattern = getStringValue(nodeConfig, "regex", "");
            String regexReplacement = getStringValue(nodeConfig, "regexReplacement", "");
            if (!regexPattern.isEmpty()) {
                try {
                    processedText = processedText.replaceAll(regexPattern, regexReplacement);
                } catch (Exception e) {
                    log.warn("Invalid regex pattern: {}", regexPattern);
                }
            }
            
            // 8. 截取操作
            Integer start = getIntegerValue(nodeConfig, "substringStart", null);
            Integer end = getIntegerValue(nodeConfig, "substringEnd", null);
            if (start != null) {
                if (end != null && end > start && end <= processedText.length()) {
                    processedText = processedText.substring(start, end);
                } else if (start < processedText.length()) {
                    processedText = processedText.substring(start);
                }
            }
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("result", processedText);
            
            return NodeOutDTO.builder()
                .success(true)
                .nodeId(node.getNodeId())
                .output(result)
                .build();
            
        } catch (Exception e) {
            log.error("Error in text editor node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("文本编辑失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 将文本转换为标题格式（每个单词首字母大写）
     */
    private String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder builder = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-' || c == '_') {
                capitalizeNext = true;
                builder.append(c);
            } else if (capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(Character.toLowerCase(c));
            }
        }
        
        return builder.toString();
    }
    
    /**
     * 获取字符串值，带默认值
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
     * 获取整数值，带默认值
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
    
    /**
     * 获取布尔值，带默认值
     */
    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
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
    private <T> List<T> getListValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) instanceof List) {
            return (List<T>) map.get(key);
        }
        return null;
    }
} 