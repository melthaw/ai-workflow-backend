package com.fastgpt.ai.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "kb_data")
public class KbData {
    @Id
    private String id;
    
    @Field("data_id")
    private String dataId;
    
    @Field("team_id")
    private String teamId;
    
    @Field("user_id")
    private String userId;
    
    @Field("kb_id")
    private String kbId;
    
    @Field("module_id")
    private String moduleId;
    
    private String q;
    
    private String a;
    
    @Field("q_tokens")
    private Integer qTokens;
    
    @Field("a_tokens")
    private Integer aTokens;
    
    private Double score;
    
    @Field("file_id")
    private String fileId;
    
    @Field("chunk_index")
    private Integer chunkIndex;
    
    @Field("chunk_size")
    private Integer chunkSize;
    
    @Field("create_time")
    private LocalDateTime createTime;
    
    @Field("update_time")
    private LocalDateTime updateTime;
    
    @Field("vector")
    private List<Float> vector;
    
    @Field("vector_model")
    private String vectorModel;
    
    private Boolean extra;
    
    @Field("collection_id")
    private String collectionId;
    
    @Field("collection_meta")
    private Map<String, String> collectionMeta;
} 