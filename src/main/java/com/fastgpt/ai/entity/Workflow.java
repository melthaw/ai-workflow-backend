package com.fastgpt.ai.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Workflow entity for MongoDB storage
 */
@Data
@Document(collection = "workflows")
public class Workflow {
    @Id
    private String id;
    
    @Field("workflow_id")
    private String workflowId;
    
    @Field("user_id")
    private String userId;
    
    @Field("team_id")
    private String teamId;
    
    @Field("app_id")
    private String appId;
    
    @Field("module_id")
    private String moduleId;
    
    private String name;
    
    private String description;
    
    @Field("create_time")
    private LocalDateTime createTime;
    
    @Field("update_time")
    private LocalDateTime updateTime;
    
    /**
     * JSON array of node definitions
     */
    private List<Map<String, Object>> nodes;
    
    /**
     * JSON array of edge definitions
     */
    private List<Map<String, Object>> edges;
    
    /**
     * Default input values for the workflow
     */
    @Field("default_inputs")
    private Map<String, Object> defaultInputs;
    
    /**
     * Additional configuration for the workflow
     */
    private Map<String, Object> config;
    
    /**
     * Workflow status: draft, published, etc.
     */
    private String status;
    
    /**
     * Whether this workflow is a template
     */
    @Field("is_template")
    private Boolean isTemplate;
} 