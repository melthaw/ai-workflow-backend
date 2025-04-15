package com.fastgpt.ai.repository;

import com.fastgpt.ai.entity.ChatFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatFile entity
 */
@Repository
public interface ChatFileRepository extends MongoRepository<ChatFile, String> {
    
    /**
     * Find a file by its unique fileId
     */
    Optional<ChatFile> findByFileId(String fileId);
    
    /**
     * Find all files belonging to a specific chat
     */
    List<ChatFile> findByChatId(String chatId);
    
    /**
     * Find all files belonging to a specific chat and user
     */
    List<ChatFile> findByChatIdAndUserId(String chatId, String userId);
    
    /**
     * Find all files belonging to a specific application
     */
    List<ChatFile> findByAppId(String appId);
    
    /**
     * Delete a file by its fileId
     */
    void deleteByFileId(String fileId);
} 