package com.fastgpt.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating an app's knowledge base configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbConfigUpdateRequest {
    
    /**
     * ID of the app to update
     */
    @NotBlank(message = "App ID cannot be empty")
    private String appId;
    
    /**
     * Whether to use knowledge bases
     */
    private Boolean useKb;
    
    /**
     * List of knowledge base IDs to associate with the app
     */
    private List<String> kbIds;
    
    /**
     * RAG configuration parameters
     * Can include: limit, minScore, and other RAG specific settings
     */
    private Map<String, Object> ragConfig;
} 