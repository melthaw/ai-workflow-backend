package com.fastgpt.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response for AI completion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompletionResponse {
    
    /**
     * AI generated response text
     */
    private String response;
    
    /**
     * Status of the request (success, error)
     */
    private String status;
    
    /**
     * Optional error message
     */
    private String error;
    
    /**
     * Optional usage information
     */
    private Map<String, Object> usage;
} 