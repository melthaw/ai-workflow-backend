package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.entity.WorkflowTemplate;
import com.fastgpt.ai.repository.WorkflowTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service to initialize workflow templates from JSON files
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateInitializer {

    private final WorkflowTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Load templates from resources once the application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadTemplatesOnStartup() {
        log.info("Loading workflow templates from resources...");
        
        try {
            // Find all template JSON files in the templates directory
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:data/templates/*.json");
            
            int loadedCount = 0;
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    // Read template from JSON
                    WorkflowTemplate template = objectMapper.readValue(is, WorkflowTemplate.class);
                    
                    // Check if template already exists
                    Optional<WorkflowTemplate> existingTemplate = templateRepository.findById(template.getTemplateId());
                    
                    if (existingTemplate.isPresent()) {
                        // Update only if resource is newer or it's an official template
                        WorkflowTemplate existing = existingTemplate.get();
                        if (template.isOfficial() && (
                                existing.getUpdateTime() == null || 
                                template.getVersion() != null && !template.getVersion().equals(existing.getVersion()))) {
                            
                            // Preserve usage count from existing template
                            template.setUsageCount(existing.getUsageCount());
                            
                            // Update timestamps
                            if (existing.getCreateTime() != null) {
                                template.setCreateTime(existing.getCreateTime());
                            } else {
                                template.setCreateTime(LocalDateTime.now());
                            }
                            template.setUpdateTime(LocalDateTime.now());
                            
                            // Save updated template
                            templateRepository.save(template);
                            log.info("Updated template: {}", template.getName());
                            loadedCount++;
                        }
                    } else {
                        // Save new template
                        if (template.getCreateTime() == null) {
                            template.setCreateTime(LocalDateTime.now());
                        }
                        if (template.getUpdateTime() == null) {
                            template.setUpdateTime(LocalDateTime.now());
                        }
                        
                        templateRepository.save(template);
                        log.info("Loaded template: {}", template.getName());
                        loadedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error loading template from {}: {}", resource.getFilename(), e.getMessage());
                }
            }
            
            log.info("Loaded {} workflow templates", loadedCount);
        } catch (IOException e) {
            log.error("Error loading workflow templates: {}", e.getMessage());
        }
    }
} 