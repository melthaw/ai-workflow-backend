package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbDataDTO {
    private String id;
    private String dataId;
    private String teamId;
    private String userId;
    private String kbId;
    private String moduleId;
    private String q;
    private String a;
    private Integer qTokens;
    private Integer aTokens;
    private Double score;
    private String fileId;
    private Integer chunkIndex;
    private Integer chunkSize;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Float> vector;
    private String vectorModel;
    private Boolean extra;
    private String collectionId;
    private Map<String, String> collectionMeta;
} 