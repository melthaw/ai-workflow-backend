package com.fastgpt.ai.service;

import java.util.Map;

/**
 * Service for chat configuration management
 */
public interface ChatConfigService {
    
    /**
     * Get model configuration for an app
     * @param appId Application ID
     * @return Map of model configuration parameters
     */
    Map<String, Object> getModelConfig(String appId);
} 