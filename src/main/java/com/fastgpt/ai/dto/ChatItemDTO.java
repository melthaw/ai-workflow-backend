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
public class ChatItemDTO {
    private String id;
    private String teamId;
    private String userId;
    private String tmbId;
    private String chatId;
    private String dataId;
    private String appId;
    private LocalDateTime time;
    private String obj;
    private String value;
    private String userFeedback;
    private String adminFeedback;
    private LocalDateTime userFeedbackTime;
    private LocalDateTime adminFeedbackTime;
    private Map<String, Object> metadata;
} 