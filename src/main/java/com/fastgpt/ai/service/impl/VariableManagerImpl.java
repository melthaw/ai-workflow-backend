package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.entity.workflow.NodeOutput;
import com.fastgpt.ai.service.VariableManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量管理器实现类
 */
@Service
public class VariableManagerImpl implements VariableManager {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");
    private static final Pattern NODE_REFERENCE_PATTERN = Pattern.compile("\\{\\{\\$(.*?)\\.(.*?)\\$}}");
    private static final List<String> SYSTEM_VARIABLE_PREFIXES = Arrays.asList(
        "userId", "teamId", "appId", "chatId", "responseChatItemId", "histories", "cTime"
    );

    private final Map<String, Object> systemVariables = new ConcurrentHashMap<>();
    private final Map<String, Object> userVariables = new ConcurrentHashMap<>();
    
    @Override
    public Object getVariable(String key) {
        if (systemVariables.containsKey(key)) {
            return systemVariables.get(key);
        }
        return userVariables.get(key);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        Object value = getVariable(key);
        if (value == null) {
            return null;
        }
        
        // Handle primitive type conversions
        if (type == String.class && !(value instanceof String)) {
            return (T) String.valueOf(value);
        } else if (type == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        } else if (type == Double.class && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        } else if (type == Boolean.class && value instanceof String) {
            return (T) Boolean.valueOf(value.toString());
        }
        
        return type.cast(value);
    }
    
    @Override
    public boolean hasVariable(String key) {
        return systemVariables.containsKey(key) || userVariables.containsKey(key);
    }
    
    @Override
    public void setUserVariable(String key, Object value) {
        userVariables.put(key, value);
    }
    
    @Override
    public void setSystemVariable(String key, Object value) {
        systemVariables.put(key, value);
    }
    
    @Override
    public Map<String, Object> getUserVariables() {
        return new HashMap<>(userVariables);
    }
    
    @Override
    public Map<String, Object> getSystemVariables() {
        return new HashMap<>(systemVariables);
    }
    
    @Override
    public Map<String, Object> getAllVariables() {
        Map<String, Object> allVariables = new HashMap<>(systemVariables);
        allVariables.putAll(userVariables); // User variables override system variables with same name
        return allVariables;
    }
    
    @Override
    public boolean removeVariable(String key) {
        if (systemVariables.containsKey(key)) {
            systemVariables.remove(key);
            return true;
        } else if (userVariables.containsKey(key)) {
            userVariables.remove(key);
            return true;
        }
        return false;
    }
    
    @Override
    public void clearUserVariables() {
        userVariables.clear();
    }
    
    @Override
    public VariableManager createScope() {
        VariableManagerImpl newScope = new VariableManagerImpl();
        // Copy system variables
        newScope.systemVariables.putAll(this.systemVariables);
        // Copy user variables
        newScope.userVariables.putAll(this.userVariables);
        return newScope;
    }
    
    /**
     * 格式化值到目标类型
     */
    @Override
    public Object formatValue(Object value, String targetType) {
        if (value == null) return null;
        
        switch (targetType) {
            case "boolean":
                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                } else if (value instanceof Boolean) {
                    return value;
                }
                return false;
            case "number":
                if (value instanceof String) {
                    try {
                        return Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0;
            case "date":
                if (value instanceof String) {
                    try {
                        return LocalDate.parse((String) value);
                    } catch (Exception e) {
                        return LocalDate.now();
                    }
                } else if (value instanceof LocalDate) {
                    return value;
                } else if (value instanceof Date) {
                    return ((Date) value).toInstant().atZone(
                        java.time.ZoneId.systemDefault()).toLocalDate();
                }
                return LocalDate.now();
            case "json":
                if (value instanceof String) {
                    return value; // 简化处理，实际需要使用JSON库解析
                } else if (value instanceof Map) {
                    return value;
                }
                return "{}";
            default:
                return String.valueOf(value);
        }
    }

    /**
     * 替换文本中的变量引用
     */
    @Override
    public Object replaceVariables(Object value, List<Node> nodes, Map<String, Object> variables) {
        if (value == null) return null;
        
        if (!(value instanceof String)) {
            return value;
        }
        
        String text = (String) value;
        
        // 替换普通变量引用 {{变量名}}
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object variableValue = variables.getOrDefault(variableName, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(variableValue)));
        }
        matcher.appendTail(result);
        
        // 替换节点引用 {{$nodeId.outputKey$}}
        String intermediateResult = result.toString();
        result = new StringBuffer();
        matcher = NODE_REFERENCE_PATTERN.matcher(intermediateResult);
        
        while (matcher.find()) {
            String nodeId = matcher.group(1);
            String outputKey = matcher.group(2);
            
            String replacement = matcher.group(0);
            for (Node node : nodes) {
                if (nodeId.equals(node.getNodeId())) {
                    for (NodeOutput output : node.getOutputs()) {
                        if (outputKey.equals(output.getKey()) && output.getValue() != null) {
                            replacement = String.valueOf(output.getValue());
                            break;
                        }
                    }
                    break;
                }
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 获取引用变量的实际值
     */
    @Override
    public Object getReferenceVariableValue(Object value, List<Node> nodes, Map<String, Object> variables) {
        if (!(value instanceof String)) {
            return value;
        }
        
        String text = (String) value;
        
        // 引用应该是完整的形式: {{$nodeId.outputKey$}}
        if (text.matches("\\{\\{\\$.*?\\.(.*?)\\$}}")) {
            Matcher matcher = NODE_REFERENCE_PATTERN.matcher(text);
            if (matcher.find()) {
                String nodeId = matcher.group(1);
                String outputKey = matcher.group(2);
                
                for (Node node : nodes) {
                    if (nodeId.equals(node.getNodeId())) {
                        for (NodeOutput output : node.getOutputs()) {
                            if (outputKey.equals(output.getKey()) && output.getValue() != null) {
                                return output.getValue();
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        return value;
    }

    /**
     * 移除系统变量
     */
    @Override
    public Map<String, Object> removeSystemVariables(Map<String, Object> variables, Map<String, Object> externalVariables) {
        Map<String, Object> result = new HashMap<>();
        
        // 复制所有变量，排除系统变量
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (!isSystemVariable(key) || (externalVariables != null && externalVariables.containsKey(key))) {
                result.put(key, entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * 判断是否为系统变量
     */
    private boolean isSystemVariable(String key) {
        return SYSTEM_VARIABLE_PREFIXES.contains(key);
    }
} 