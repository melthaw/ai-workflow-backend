package com.fastgpt.ai.repository;

import com.fastgpt.ai.entity.WorkflowTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for workflow templates
 */
@Repository
public interface WorkflowTemplateRepository extends MongoRepository<WorkflowTemplate, String> {
    
    /**
     * Find templates by category
     * @param category Category name
     * @return List of matching templates
     */
    List<WorkflowTemplate> findByCategory(String category);
    
    /**
     * Find official templates
     * @return List of official templates
     */
    List<WorkflowTemplate> findByOfficialTrue();
    
    /**
     * Find templates created by a user
     * @param userId User ID
     * @return List of matching templates
     */
    List<WorkflowTemplate> findByCreatedBy(String userId);
    
    /**
     * Find template by ID
     * @param templateId Template ID
     * @return Optional containing the template if found
     */
    Optional<WorkflowTemplate> findById(String templateId);
    
    /**
     * Get all distinct categories
     * @return List of category names
     */
    @Query(value = "{}", fields = "{ 'category': 1 }")
    List<String> findAllCategories();
    
    /**
     * Increment the usage count of a template
     * @param templateId Template ID
     */
    @Query("{ 'templateId': ?0 }")
    @Update("{ '$inc': { 'usageCount': 1 } }")
    void incrementUsageCount(String templateId);
    
    /**
     * Find templates by tag
     * @param tag Tag name
     * @return List of matching templates
     */
    @Query("{ 'tags': ?0 }")
    List<WorkflowTemplate> findByTag(String tag);
    
    /**
     * Search templates by name or description
     * @param keyword Search keyword
     * @return List of matching templates
     */
    @Query("{ $or: [ { 'name': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    List<WorkflowTemplate> searchByKeyword(String keyword);
} 