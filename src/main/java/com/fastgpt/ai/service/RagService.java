package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.KbDataDTO;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Service for Retrieval Augmented Generation (RAG)
 */
public interface RagService {
    
    /**
     * Get a RAG response for a user query
     * @param query User query
     * @param kbIds List of knowledge base IDs to search
     * @param extraParams Additional parameters (limit, minScore, systemPrompt, etc.)
     * @return Map containing the answer, sources, and other metadata
     */
    Map<String, Object> getRagResponse(String query, List<String> kbIds, Map<String, Object> extraParams);
    
    /**
     * Stream a RAG response for a user query
     * @param query User query
     * @param kbIds List of knowledge base IDs to search
     * @param extraParams Additional parameters (should include "streaming": true)
     * @param chunkConsumer Callback to receive text chunks, along with a flag indicating if it's the last chunk
     */
    void streamRagResponse(String query, List<String> kbIds, Map<String, Object> extraParams, 
                         BiConsumer<String, Boolean> chunkConsumer);
    
    /**
     * Get metadata for a RAG query without generating a full response
     * Used to get sources and other details
     * @param query User query
     * @param kbIds List of knowledge base IDs to search
     * @param extraParams Additional parameters
     * @return Map containing the sources and other metadata
     */
    Map<String, Object> getRagMetadata(String query, List<String> kbIds, Map<String, Object> extraParams);
} 