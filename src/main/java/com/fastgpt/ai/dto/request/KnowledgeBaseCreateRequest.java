package com.fastgpt.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseCreateRequest {
    @NotBlank(message = "User ID cannot be empty")
    private String userId;
    
    private String teamId;
    
    @NotBlank(message = "Name cannot be empty")
    private String name;
    
    private String tags;
    
    private String intro;
    
    @NotBlank(message = "Vector model cannot be empty")
    private String vectorModel;
    
    private String collectionId;
    
    private Boolean shared = false;
    
    private Map<String, Object> customInfo;
    
    private Map<String, Object> modelInfo;
} 