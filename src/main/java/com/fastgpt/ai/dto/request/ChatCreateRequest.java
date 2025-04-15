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
public class ChatCreateRequest {
    @NotBlank(message = "User ID cannot be empty")
    private String userId;
    
    private String teamId;
    
    private String tmbId;
    
    @NotBlank(message = "App ID cannot be empty")
    private String appId;
    
    private String title;
    
    private String customTitle;
    
    private String source;
    
    private String sourceName;
    
    private List<Map<String, Object>> variableList;
    
    private String welcomeText;
    
    private Map<String, Object> variables;
    
    private Map<String, Object> pluginInputs;
    
    private Map<String, Object> metadata;
} 