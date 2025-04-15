package com.fastgpt.ai.repository;

import com.fastgpt.ai.entity.App;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppRepository extends MongoRepository<App, String> {
    
    Optional<App> findByAppId(String appId);
    
    List<App> findByUserId(String userId);
    
    List<App> findByTeamId(String teamId);
    
    List<App> findByUserIdOrTeamId(String userId, String teamId);
    
    void deleteByAppId(String appId);
} 