package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.SearchDataResponseItemDTO;
import com.fastgpt.ai.dto.VectorSearchRequestDTO;
import com.fastgpt.ai.dto.KbDataDTO;

import java.util.List;
import java.util.Map;

/**
 * Service for vector operations and embedding
 */
public interface VectorService {
    
    /**
     * Generate a vector embedding for text
     * @param text The text to embed
     * @param model Optional embedding model name
     * @return List of float values representing the embedding vector
     */
    List<Float> generateEmbedding(String text, String model);
    
    /**
     * Search for similar vectors in the database
     * @param request Vector search request
     * @return List of search results
     */
    List<KbDataDTO> search(VectorSearchRequestDTO request);
    
    /**
     * Count tokens in a text
     * @param text The text to count tokens in
     * @return The number of tokens
     */
    int countTokens(String text);
    
    /**
     * Search for similar content using query text
     * @param query The query text
     * @param datasetIds List of dataset IDs to search in
     * @param similarityThreshold Minimum similarity score (0-1)
     * @return List of search results with scores
     */
    List<SearchDataResponseItemDTO> searchSimilarVectors(String query, List<String> datasetIds, double similarityThreshold);
    
    /**
     * Search for similar content using query text with pagination
     * @param query The query text
     * @param datasetIds List of dataset IDs to search in
     * @param similarityThreshold Minimum similarity score (0-1)
     * @param limit Maximum number of results
     * @param offset Starting offset for pagination
     * @return List of search results with scores
     */
    List<SearchDataResponseItemDTO> searchSimilarVectors(
        String query, 
        List<String> datasetIds, 
        double similarityThreshold,
        int limit,
        int offset
    );
    
    /**
     * Search for similar content with metadata filtering
     * @param query The query text
     * @param datasetIds List of dataset IDs to search in
     * @param similarityThreshold Minimum similarity score (0-1)
     * @param metadataFilters Map of metadata field names to values for filtering
     * @param limit Maximum number of results
     * @return List of search results with scores
     */
    List<SearchDataResponseItemDTO> searchSimilarVectorsWithMetadata(
        String query, 
        List<String> datasetIds, 
        double similarityThreshold,
        Map<String, List<String>> metadataFilters,
        int limit
    );
} 