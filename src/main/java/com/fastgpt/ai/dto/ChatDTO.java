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
public class ChatDTO {
    private String id;
    private String chatId;
    private String userId;
    private String teamId;
    private String tmbId;
    private String appId;
    private LocalDateTime updateTime;
    private String title;
    private String customTitle;
    private Boolean top;
    private String source;
    private String sourceName;
    private String shareId;
    private String outLinkUid;
    private List<Map<String, Object>> variableList;
    private String welcomeText;
    private Map<String, Object> variables;
    private Map<String, Object> pluginInputs;
    private Map<String, Object> metadata;
} 