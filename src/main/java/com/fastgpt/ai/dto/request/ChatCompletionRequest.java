package com.fastgpt.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request body for chat completions API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {
    
    /**
     * ID of the chat to send the message to
     */
    @NotBlank(message = "Chat ID is required")
    private String chatId;
    
    /**
     * ID of the app to use for this chat
     */
    private String appId;
    
    /**
     * User's message content
     */
    @NotBlank(message = "Message is required")
    private String message;
    
    /**
     * User ID (anonymized if not authenticated)
     */
    private String userId;
    
    /**
     * Team ID if applicable
     */
    private String teamId;
    
    /**
     * Whether to stream the response
     */
    private boolean stream;
    
    /**
     * Whether to use RAG for this message
     */
    private boolean useRag;
    
    /**
     * List of knowledge base IDs to search
     */
    private List<String> kbIds;
    
    /**
     * Maximum number of search results to use
     */
    private Integer limit;
    
    /**
     * Minimum similarity score for search results
     */
    private Double minScore;
    
    /**
     * Additional metadata for the request
     */
    private Map<String, Object> metadata;
} 