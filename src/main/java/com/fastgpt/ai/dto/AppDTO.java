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
public class AppDTO {
    private String id;
    private String appId;
    private String teamId;
    private String userId;
    private String name;
    private String avatar;
    private String intro;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
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