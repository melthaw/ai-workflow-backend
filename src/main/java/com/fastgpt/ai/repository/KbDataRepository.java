package com.fastgpt.ai.repository;

import com.fastgpt.ai.entity.KbData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KbDataRepository extends MongoRepository<KbData, String> {
    
    Optional<KbData> findByDataId(String dataId);
    
    List<KbData> findByKbId(String kbId);
    
    List<KbData> findByKbIdAndUserId(String kbId, String userId);
    
    List<KbData> findByFileId(String fileId);
    
    List<KbData> findByCollectionId(String collectionId);
    
    long countByKbId(String kbId);
    
    void deleteByDataId(String dataId);
    
    void deleteByKbId(String kbId);
    
    void deleteByFileId(String fileId);
} 