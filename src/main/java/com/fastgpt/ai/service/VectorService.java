package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.KbDataDTO;
import com.fastgpt.ai.dto.request.VectorSearchRequest;

import java.util.List;

/**
 * Service for vector operations (embedding, search)
 */
public interface VectorService {
    
    /**
     * Generate embeddings for a text
     * @param text Text to embed
     * @param model Vector model to use
     * @return Embedding vector
     */
    List<Float> generateEmbedding(String text, String model);
    
    /**
     * Calculate the similarity between two vectors
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Similarity score (0-1)
     */
    double calculateSimilarity(List<Float> vector1, List<Float> vector2);
    
    /**
     * Search for similar items in the knowledge base
     * @param request Vector search request
     * @return List of similar items with scores
     */
    List<KbDataDTO> search(VectorSearchRequest request);
    
    /**
     * Count tokens in a text
     * @param text Text to count tokens for
     * @return Token count
     */
    int countTokens(String text);
} 