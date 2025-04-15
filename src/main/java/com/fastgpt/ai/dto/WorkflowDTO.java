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
public class WorkflowDTO {
    private String id;
    private String workflowId;
    private String teamId;
    private String userId;
    private String name;
    private String description;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;
    private Map<String, Object> variables;
    private String moduleId;
    private String appId;
    private Boolean isTemplate;
} 