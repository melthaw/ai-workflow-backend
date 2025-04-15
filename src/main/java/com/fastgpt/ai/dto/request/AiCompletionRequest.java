package com.fastgpt.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request for AI completion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompletionRequest {
    
    /**
     * Single prompt message content
     */
    private String prompt;
    
    /**
     * Array of message objects with role and content
     */
    private List<Map<String, String>> messages;
    
    /**
     * System prompt to guide AI behavior
     */
    private String systemPrompt;
    
    /**
     * Model name to use
     */
    private String model;
    
    /**
     * Temperature parameter (0.0 to 1.0)
     */
    private Float temperature;
    
    /**
     * Maximum tokens to generate
     */
    private Integer maxTokens;
} 