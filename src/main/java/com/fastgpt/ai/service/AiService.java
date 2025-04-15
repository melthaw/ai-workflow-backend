package com.fastgpt.ai.service;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Service for AI integration
 */
public interface AiService {
    
    /**
     * Generate a response from an AI model
     * @param messages List of message maps with 'role' and 'content' fields
     * @param systemPrompt System prompt to guide the AI
     * @param modelConfig Model configuration options
     * @return The AI generated response text
     */
    String generateResponse(List<Map<String, String>> messages, String systemPrompt, Map<String, Object> modelConfig);
    
    /**
     * Generate a streaming response from an AI model, delivering results in chunks
     * @param messages List of message maps with 'role' and 'content' fields
     * @param systemPrompt System prompt to guide the AI
     * @param modelConfig Model configuration options
     * @param chunkConsumer Callback to receive text chunks, along with a flag indicating if it's the last chunk
     */
    void generateStreamingResponse(List<Map<String, String>> messages, String systemPrompt, 
                                  Map<String, Object> modelConfig, BiConsumer<String, Boolean> chunkConsumer);
    
    /**
     * Simple method to generate a response from a single user prompt
     * @param prompt The user's prompt
     * @param appId Optional app ID for configuration
     * @return The AI generated response text
     */
    String generateSimpleResponse(String prompt, String appId);
} 