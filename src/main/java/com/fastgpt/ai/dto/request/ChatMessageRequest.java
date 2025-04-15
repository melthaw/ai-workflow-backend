package com.fastgpt.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request for sending chat messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    
    /**
     * ID of the chat to send the message to
     */
    private String chatId;
    
    /**
     * ID of the app to use for this chat
     */
    private String appId;
    
    /**
     * User message content
     */
    private String message;
    
    /**
     * User ID
     */
    private String userId;
    
    /**
     * Team ID
     */
    private String teamId;
    
    /**
     * Team member ID
     */
    private String tmbId;
    
    /**
     * Whether to use RAG for this message
     */
    private Boolean useRag;
    
    /**
     * List of knowledge base IDs to search
     */
    private List<String> kbIds;
    
    /**
     * Maximum number of search results
     */
    private Integer limit;
    
    /**
     * Minimum similarity score threshold
     */
    private Double minScore;
    
    /**
     * Additional metadata for the request
     */
    private Map<String, Object> metadata;
} 