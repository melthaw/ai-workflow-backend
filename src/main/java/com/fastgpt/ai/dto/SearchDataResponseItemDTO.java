package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for search result data from vector database
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDataResponseItemDTO {
    
    /**
     * Unique identifier
     */
    private String id;
    
    /**
     * Team ID
     */
    private String teamId;
    
    /**
     * User ID
     */
    private String userId;
    
    /**
     * Knowledge base ID
     */
    private String kbId;
    
    /**
     * Module ID
     */
    private String moduleId;
    
    /**
     * Question or query text
     */
    private String q;
    
    /**
     * Answer or content text
     */
    private String a;
    
    /**
     * File ID if from a file
     */
    private String fileId;
    
    /**
     * Index in the file chunk
     */
    private Integer chunkIndex;
    
    /**
     * Creation time
     */
    private LocalDateTime createTime;
    
    /**
     * Update time
     */
    private LocalDateTime updateTime;
    
    /**
     * Similarity score
     */
    private Double score;
    
    /**
     * Vector model used
     */
    private String vectorModel;
    
    /**
     * Source name (filename, etc)
     */
    private String sourceName;
    
    /**
     * Collection ID
     */
    private String collectionId;
} 