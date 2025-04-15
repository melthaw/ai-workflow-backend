package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.entity.App;
import com.fastgpt.ai.repository.AppRepository;
import com.fastgpt.ai.service.ChatConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of chat configuration service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatConfigServiceImpl implements ChatConfigService {

    private final AppRepository appRepository;
    
    @Override
    public Map<String, Object> getModelConfig(String appId) {
        if (appId == null || appId.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Try to retrieve app from repository
        Optional<App> appOptional = appRepository.findByAppId(appId);
        
        if (appOptional.isEmpty()) {
            log.debug("App not found for ID: {}", appId);
            return Collections.emptyMap();
        }
        
        App app = appOptional.get();
        
        // Build and return model configuration
        Map<String, Object> modelConfig = new HashMap<>();
        
        // Add default settings
        modelConfig.put("model", "gpt-3.5-turbo");
        modelConfig.put("temperature", 0.7);
        modelConfig.put("max_tokens", 4096);
        
        // Add configuration from app metadata if available
        if (app.getMetadata() != null) {
            Map<String, Object> metadata = app.getMetadata();
            
            // Check for model configuration in metadata
            if (metadata.containsKey("model")) {
                modelConfig.put("model", metadata.get("model"));
            }
            
            // Check for model parameters
            if (metadata.containsKey("temperature")) {
                modelConfig.put("temperature", metadata.get("temperature"));
            }
            
            if (metadata.containsKey("max_tokens")) {
                modelConfig.put("max_tokens", metadata.get("max_tokens"));
            }
        }
        
        return modelConfig;
    }
} 