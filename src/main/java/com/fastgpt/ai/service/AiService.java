package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.function.FunctionCallResponse;
import com.fastgpt.ai.dto.function.FunctionDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Service for interacting with AI models
 */
public interface AiService {
    
    /**
     * Generate a simple text response
     * 
     * @param prompt The input prompt
     * @param systemPrompt Optional system prompt
     * @return Generated text response
     */
    String generateSimpleResponse(String prompt, String systemPrompt);
    
    /**
     * Generate a chat response using specified parameters
     * 
     * @param prompt The input prompt
     * @param systemPrompt Optional system prompt
     * @param model The model to use
     * @param temperature The temperature parameter (0-1)
     * @param maxTokens Maximum tokens to generate
     * @return Generated text response
     */
    String generateResponse(String prompt, String systemPrompt, String model, 
                            Double temperature, Integer maxTokens);
    
    /**
     * Generate a response with function calling capabilities
     * 
     * @param prompt The user prompt
     * @param functions Available functions
     * @param model The model to use
     * @return Future containing function call response
     */
    CompletableFuture<FunctionCallResponse> generateWithFunctions(String prompt, 
            List<FunctionDefinition> functions, String model);
    
    /**
     * Generate a response with function calling capabilities
     * 
     * @param prompt The user prompt
     * @param systemPrompt Optional system prompt
     * @param functions Available functions
     * @param model The model to use
     * @param temperature The temperature parameter (0-1)
     * @return Future containing function call response
     */
    CompletableFuture<FunctionCallResponse> generateWithFunctions(String prompt, String systemPrompt,
            List<FunctionDefinition> functions, String model, Double temperature);
    
    /**
     * Get list of available models
     * 
     * @return List of available model identifiers
     */
    List<String> getAvailableModels();
    
    /**
     * Get information about a specific model
     * 
     * @param modelId The model identifier
     * @return Model information
     */
    Map<String, Object> getModelInfo(String modelId);
    
    /**
     * Calculate token usage for a given text
     * 
     * @param text The text to calculate token usage for
     * @param model The model to use for calculation
     * @return Number of tokens
     */
    int calculateTokenUsage(String text, String model);
} 