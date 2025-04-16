package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.service.VariableManager;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of VariableManager that separates system and user variables
 */
@Service
public class VariableManagerImpl implements VariableManager {
    
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
    
    @Override
    public Object formatValue(Object value, String targetType) {
        if (value == null) {
            return null;
        }
        
        switch (targetType.toLowerCase()) {
            case "string":
                return value.toString();
            case "number":
                if (value instanceof String) {
                    try {
                        return Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0.0;
            case "boolean":
                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                } else if (value instanceof Boolean) {
                    return value;
                }
                return false;
            case "object":
                // Already an object, no conversion needed
                return value;
            case "array":
                if (value instanceof Object[]) {
                    return value;
                } else if (value instanceof Iterable) {
                    return value;
                } else {
                    // Convert to single-element array
                    return Collections.singletonList(value);
                }
            default:
                return value;
        }
    }
} 