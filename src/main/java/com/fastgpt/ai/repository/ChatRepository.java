package com.fastgpt.ai.repository;

import com.fastgpt.ai.entity.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends MongoRepository<Chat, String> {
    
    List<Chat> findByUserId(String userId);
    
    List<Chat> findByAppId(String appId);
    
    List<Chat> findByUserIdAndAppId(String userId, String appId);
    
    Optional<Chat> findByChatId(String chatId);
    
    void deleteByChatId(String chatId);
} 