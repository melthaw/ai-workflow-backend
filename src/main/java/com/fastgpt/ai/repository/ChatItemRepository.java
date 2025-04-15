package com.fastgpt.ai.repository;

import com.fastgpt.ai.entity.ChatItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatItemRepository extends MongoRepository<ChatItem, String> {
    
    List<ChatItem> findByChatId(String chatId);
    
    Page<ChatItem> findByChatId(String chatId, Pageable pageable);
    
    List<ChatItem> findByAppId(String appId);
    
    List<ChatItem> findByUserId(String userId);
    
    void deleteByChatId(String chatId);
} 