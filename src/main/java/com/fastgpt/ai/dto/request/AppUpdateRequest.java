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
public class AppUpdateRequest {
    @NotBlank(message = "App ID cannot be empty")
    private String appId;
    
    private String name;
    
    private String avatar;
    
    private String intro;
    
    private Boolean publish;
    
    private Boolean free;
    
    private List<Map<String, Object>> chatModels;
    
    private String systemPrompt;
    
    private Map<String, Object> variables;
    
    private Map<String, Object> modelConfig;
    
    private String workflowId;
    
    private Boolean useWorkflow;
    
    private List<Map<String, Object>> modules;
    
    private List<String> kbIds;
    
    private Boolean useKb;
    
    private Map<String, Object> ragConfig;
} 