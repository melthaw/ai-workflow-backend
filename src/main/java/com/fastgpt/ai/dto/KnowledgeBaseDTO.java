package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDTO {
    private String id;
    private String kbId;
    private String teamId;
    private String userId;
    private String name;
    private String tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String intro;
    private String vectorModel;
    private Integer fileCount;
    private Integer kbDataCount;
    private String collectionId;
    private Boolean shared;
    private Map<String, Object> customInfo;
    private Map<String, Object> modelInfo;
} 