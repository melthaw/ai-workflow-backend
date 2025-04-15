package com.fastgpt.ai.repository;

import com.fastgpt.ai.entity.Workflow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {
    
    Optional<Workflow> findByWorkflowId(String workflowId);
    
    List<Workflow> findByUserId(String userId);
    
    List<Workflow> findByTeamId(String teamId);
    
    List<Workflow> findByUserIdOrTeamId(String userId, String teamId);
    
    List<Workflow> findByAppId(String appId);
    
    List<Workflow> findByModuleId(String moduleId);
    
    List<Workflow> findByIsTemplate(Boolean isTemplate);
    
    List<Workflow> findByUserIdAndStatus(String userId, String status);
    
    void deleteByWorkflowId(String workflowId);
} 