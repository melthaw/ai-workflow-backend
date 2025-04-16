package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for vector search request parameters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchRequestDTO {
    
    /**
     * The query text to search for
     */
    private String query;
    
    /**
     * The pre-computed query vector (optional, will be generated from query if not provided)
     */
    private List<Float> queryVector;
    
    /**
     * List of dataset IDs to search in
     */
    private List<String> datasetIds;
    
    /**
     * Minimum similarity score threshold (0-1)
     */
    private Double minScore;
    
    /**
     * Maximum number of results to return
     */
    private Integer limit;
    
    /**
     * Offset for pagination
     */
    private Integer offset;
    
    /**
     * Vector model to use for embedding
     */
    private String model;
    
    /**
     * Metadata field filters
     * Map of field names to list of acceptable values
     */
    private Map<String, List<String>> metadataFilters;
    
    /**
     * Search mode: similarity, hybrid, etc.
     */
    private String searchMode;
    
    /**
     * Whether to use re-ranking
     */
    private Boolean useReRank;
    
    /**
     * Extended context for hybrid search
     */
    private String context;
} 