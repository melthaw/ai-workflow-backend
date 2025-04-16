package com.fastgpt.ai.entity;

import com.fastgpt.ai.dto.workflow.ConnectionDTO;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entity for workflow templates
 */
@Data
@Document(collection = "workflowTemplates")
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplate {
    
    /**
     * Template ID
     */
    @Id
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