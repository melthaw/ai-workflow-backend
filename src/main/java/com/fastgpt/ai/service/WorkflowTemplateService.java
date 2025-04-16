package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.workflow.WorkflowTemplateDTO;

import java.util.List;
import java.util.Map;

/**
 * Service for managing workflow templates
 */
public interface WorkflowTemplateService {
    
    /**
     * Get all available workflow templates
     * @return List of workflow template DTOs
     */
    List<WorkflowTemplateDTO> getAllTemplates();
    
    /**
     * Get workflow templates by category
     * @param category Template category
     * @return List of workflow template DTOs in the specified category
     */
    List<WorkflowTemplateDTO> getTemplatesByCategory(String category);
    
    /**
     * Get a workflow template by ID
     * @param templateId Template ID
     * @return Workflow template DTO
     */
    WorkflowTemplateDTO getTemplateById(String templateId);
    
    /**
     * Create a new workflow from a template
     * @param templateId Template ID
     * @param parameters Parameters to customize the template
     * @param userId User ID of the creator
     * @param teamId Optional team ID
     * @param workflowName Optional custom name for the new workflow
     * @return The created workflow DTO
     */
    WorkflowDTO instantiateTemplate(
        String templateId, 
        Map<String, Object> parameters,
        String userId,
        String teamId,
        String workflowName
    );
    
    /**
     * Create a new workflow template from an existing workflow
     * @param workflowId Workflow ID to convert to template
     * @param category Template category
     * @param templateName Name for the template
     * @param description Description of the template
     * @param parameterDefinitions Definitions of customizable parameters
     * @return Created template DTO
     */
    WorkflowTemplateDTO createTemplateFromWorkflow(
        String workflowId,
        String category,
        String templateName,
        String description,
        Map<String, Object> parameterDefinitions
    );
    
    /**
     * Update an existing template
     * @param templateId Template ID
     * @param category Updated category
     * @param templateName Updated name
     * @param description Updated description
     * @param parameterDefinitions Updated parameter definitions
     * @return Updated template DTO
     */
    WorkflowTemplateDTO updateTemplate(
        String templateId,
        String category,
        String templateName,
        String description,
        Map<String, Object> parameterDefinitions
    );
    
    /**
     * Delete a template
     * @param templateId Template ID
     */
    void deleteTemplate(String templateId);
    
    /**
     * Get all available template categories
     * @return List of category names
     */
    List<String> getAllCategories();
} 