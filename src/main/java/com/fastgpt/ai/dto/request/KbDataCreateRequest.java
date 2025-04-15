package com.fastgpt.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbDataCreateRequest {
    @NotBlank(message = "User ID cannot be empty")
    private String userId;
    
    private String teamId;
    
    @NotBlank(message = "KB ID cannot be empty")
    private String kbId;
    
    private String moduleId;
    
    @NotBlank(message = "Question cannot be empty")
    private String q;
    
    private String a;
    
    private Double score;
    
    private String fileId;
    
    private Integer chunkIndex;
    
    private Integer chunkSize;
    
    private List<Float> vector;
    
    private String vectorModel;
    
    private Boolean extra = false;
    
    private String collectionId;
    
    private Map<String, String> collectionMeta;
} 