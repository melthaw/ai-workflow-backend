package com.fastgpt.ai.repository;

import com.fastgpt.ai.entity.KnowledgeBase;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends MongoRepository<KnowledgeBase, String> {
    
    Optional<KnowledgeBase> findByKbId(String kbId);
    
    List<KnowledgeBase> findByUserId(String userId);
    
    List<KnowledgeBase> findByUserIdOrSharedIsTrue(String userId);
    
    List<KnowledgeBase> findByTeamId(String teamId);
    
    void deleteByKbId(String kbId);
} 