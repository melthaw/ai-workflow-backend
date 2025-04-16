package com.fastgpt.ai.dto.openai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a message in a chat completion.
 * Used for OpenAI chat completion API.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    
    /**
     * The role of the author of this message.
     * Must be one of: 'system', 'user', or 'assistant'.
     */
    private String role;
    
    /**
     * The content of the message.
     */
    private String content;
} 