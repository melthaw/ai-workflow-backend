package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.KbDataDTO;
import com.fastgpt.ai.dto.KnowledgeBaseDTO;
import com.fastgpt.ai.dto.request.KbDataCreateRequest;
import com.fastgpt.ai.dto.request.KnowledgeBaseCreateRequest;
import com.fastgpt.ai.dto.request.VectorSearchRequest;

import java.util.List;

/**
 * Service for knowledge base operations
 */
public interface KnowledgeBaseService {
    
    /**
     * Create a new knowledge base
     * @param request Knowledge base creation request
     * @return The created knowledge base DTO
     */
    KnowledgeBaseDTO createKnowledgeBase(KnowledgeBaseCreateRequest request);
    
    /**
     * Get a knowledge base by ID
     * @param kbId Knowledge base ID
     * @return Knowledge base DTO
     */
    KnowledgeBaseDTO getKnowledgeBaseById(String kbId);
    
    /**
     * Get all knowledge bases by user ID
     * @param userId User ID
     * @return List of knowledge base DTOs
     */
    List<KnowledgeBaseDTO> getKnowledgeBasesByUserId(String userId);
    
    /**
     * Get all knowledge bases accessible to a user (personal and shared)
     * @param userId User ID
     * @return List of knowledge base DTOs
     */
    List<KnowledgeBaseDTO> getAccessibleKnowledgeBases(String userId);
    
    /**
     * Update a knowledge base
     * @param kbId Knowledge base ID
     * @param update Updated knowledge base DTO
     * @return The updated knowledge base DTO
     */
    KnowledgeBaseDTO updateKnowledgeBase(String kbId, KnowledgeBaseDTO update);
    
    /**
     * Delete a knowledge base by ID
     * @param kbId Knowledge base ID
     */
    void deleteKnowledgeBase(String kbId);
    
    /**
     * Add data to a knowledge base
     * @param request KB data creation request
     * @return The created KB data DTO
     */
    KbDataDTO addData(KbDataCreateRequest request);
    
    /**
     * Get knowledge base data by ID
     * @param dataId Data ID
     * @return KB data DTO
     */
    KbDataDTO getDataById(String dataId);
    
    /**
     * Get all data in a knowledge base
     * @param kbId Knowledge base ID
     * @return List of KB data DTOs
     */
    List<KbDataDTO> getDataByKbId(String kbId);
    
    /**
     * Delete knowledge base data by ID
     * @param dataId Data ID
     */
    void deleteData(String dataId);
    
    /**
     * Search the knowledge base
     * @param request Vector search request
     * @return List of similar KB data DTOs
     */
    List<KbDataDTO> search(VectorSearchRequest request);
} 