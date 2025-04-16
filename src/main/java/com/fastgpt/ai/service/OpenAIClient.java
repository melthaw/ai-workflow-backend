package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.openai.ChatCompletionRequest;
import com.fastgpt.ai.dto.openai.OpenAIResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Client interface for interacting with OpenAI API
 */
public interface OpenAIClient {
    
    /**
     * Create a chat completion using OpenAI API
     * 
     * @param request Chat completion request
     * @return CompletableFuture containing the OpenAI response
     */
    CompletableFuture<OpenAIResponse> createChatCompletion(ChatCompletionRequest request);
    
    /**
     * Create embeddings for text
     * 
     * @param text Text to embed
     * @param model Model to use for embedding
     * @return CompletableFuture containing the embedding response
     */
    CompletableFuture<float[]> createEmbedding(String text, String model);
    
    /**
     * Create embeddings for multiple texts
     * 
     * @param texts Texts to embed
     * @param model Model to use for embedding
     * @return CompletableFuture containing the embedding responses
     */
    CompletableFuture<float[][]> createEmbeddings(String[] texts, String model);
} 