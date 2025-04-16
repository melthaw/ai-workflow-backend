package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for workflow templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplateDTO {
    
    /**
     * Template ID
     */
    private String templateId;
    
    /**
     * Template name
     */
    private String name;
    
    /**
     * Template description
     */
    private String description;
    
    /**
     * Template category
     */
    private String category;
    
    /**
     * Template version
     */
    private String version;
    
    /**
     * Tags for the template
     */
    private List<String> tags;
    
    /**
     * Creator user ID
     */
    private String createdBy;
    
    /**
     * Creation date
     */
    private LocalDateTime createTime;
    
    /**
     * Last update date
     */
    private LocalDateTime updateTime;
    
    /**
     * Is official template (provided by FastGPT)
     */
    private boolean official;
    
    /**
     * Workflow definition - nodes
     */
    private List<NodeDefDTO> nodes;
    
    /**
     * Workflow definition - connections
     */
    private List<ConnectionDTO> edges;
    
    /**
     * Default inputs for the workflow
     */
    private Map<String, Object> defaultInputs;
    
    /**
     * Parameter definitions that can be customized when instantiating
     */
    private Map<String, Object> parameterDefinitions;
    
    /**
     * Template thumbnail image URL
     */
    private String thumbnailUrl;
    
    /**
     * Usage count
     */
    private Long usageCount;
    
    /**
     * Additional template configuration
     */
    private Map<String, Object> config;
} 