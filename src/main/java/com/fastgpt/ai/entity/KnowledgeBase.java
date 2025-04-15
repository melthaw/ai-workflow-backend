package com.fastgpt.ai.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "knowledge_base")
public class KnowledgeBase {
    @Id
    private String id;
    
    @Field("kb_id")
    private String kbId;
    
    @Field("team_id")
    private String teamId;
    
    @Field("user_id")
    private String userId;
    
    private String name;
    
    private String tags;
    
    @Field("create_time")
    private LocalDateTime createTime;
    
    @Field("update_time")
    private LocalDateTime updateTime;
    
    private String intro;
    
    @Field("vector_model")
    private String vectorModel;
    
    @Field("file_count")
    private Integer fileCount;
    
    @Field("kb_data_count")
    private Integer kbDataCount;
    
    @Field("collection_id")
    private String collectionId;
    
    private Boolean shared;
    
    @Field("custom_info")
    private Map<String, Object> customInfo;
    
    @Field("model_info")
    private Map<String, Object> modelInfo;
} 