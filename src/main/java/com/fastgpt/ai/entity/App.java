package com.fastgpt.ai.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entity representing an application
 */
@Data
@Document(collection = "apps")
public class App {

    @Id
    private String id;
    
    @Field("app_id")
    private String appId;
    
    @Field("team_id")
    private String teamId;
    
    @Field("user_id")
    private String userId;
    
    private String name;
    
    @Field("avatar")
    private String avatar;
    
    @Field("create_time")
    private LocalDateTime createTime;
    
    @Field("update_time")
    private LocalDateTime updateTime;
    
    @Field("type")
    private String type;
    
    @Field("use_workflow")
    private Boolean useWorkflow;
    
    @Field("workflow_id")
    private String workflowId;
    
    @Field("system_prompt")
    private String systemPrompt;
    
    @Field("use_kb")
    private Boolean useKb;
    
    @Field("kb_ids")
    private List<String> kbIds;
    
    @Field("variables")
    private Map<String, Object> variables;
    
    @Field("rag_config")
    private Map<String, Object> ragConfig;
    
    @Field("metadata")
    private Map<String, Object> metadata;
} 