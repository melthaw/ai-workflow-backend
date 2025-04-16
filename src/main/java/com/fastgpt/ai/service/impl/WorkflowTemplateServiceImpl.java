package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.workflow.WorkflowTemplateDTO;
import com.fastgpt.ai.dto.request.WorkflowCreateRequest;
import com.fastgpt.ai.entity.WorkflowTemplate;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.mapper.WorkflowTemplateMapper;
import com.fastgpt.ai.repository.WorkflowTemplateRepository;
import com.fastgpt.ai.service.WorkflowService;
import com.fastgpt.ai.service.WorkflowTemplateService;
import com.fastgpt.ai.service.VariableManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the workflow template service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTemplateServiceImpl implements WorkflowTemplateService {

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowTemplateMapper templateMapper;
    private final WorkflowService workflowService;
    private final VariableManager variableManager;
    private final ObjectMapper objectMapper;

    @Override
    public List<WorkflowTemplateDTO> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(templateMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowTemplateDTO> getTemplatesByCategory(String category) {
        return templateRepository.findByCategory(category).stream()
                .map(templateMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowTemplateDTO getTemplateById(String templateId) {
        WorkflowTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTemplate", "templateId", templateId));
        return templateMapper.toDTO(template);
    }

    @Override
    @Transactional
    public WorkflowDTO instantiateTemplate(
            String templateId,
            Map<String, Object> parameters,
            String userId,
            String teamId,
            String workflowName) {

        // Get the template
        WorkflowTemplateDTO template = getTemplateById(templateId);
        
        // Apply parameters to template
        WorkflowDTO workflowDTO = applyParametersToTemplate(template, parameters);
        
        // Create workflow request
        WorkflowCreateRequest createRequest = new WorkflowCreateRequest();
        createRequest.setName(workflowName != null ? workflowName : template.getName() + " (from template)");
        createRequest.setDescription(template.getDescription());
        createRequest.setNodes(workflowDTO.getNodes());
        createRequest.setEdges(workflowDTO.getEdges());
        createRequest.setUserId(userId);
        createRequest.setTeamId(teamId);
        createRequest.setDefaultInputs(template.getDefaultInputs());
        
        // Store template information in workflow metadata
        Map<String, Object> config = new HashMap<>();
        config.put("templateId", templateId);
        config.put("templateVersion", template.getVersion());
        config.put("instantiatedAt", LocalDateTime.now());
        config.put("instantiatedBy", userId);
        createRequest.setConfig(config);
        
        // Log the template instantiation
        log.info("Creating workflow from template: {} for user: {}", templateId, userId);
        
        // Create the workflow
        WorkflowDTO createdWorkflow = workflowService.createWorkflow(createRequest);
        
        // Update template usage count
        updateTemplateUsageCount(templateId);
        
        return createdWorkflow;
    }

    @Override
    @Transactional
    public WorkflowTemplateDTO createTemplateFromWorkflow(
            String workflowId,
            String category,
            String templateName,
            String description,
            Map<String, Object> parameterDefinitions) {
        
        // Get the workflow
        WorkflowDTO workflow = workflowService.getWorkflowById(workflowId);
        
        // Create template entity
        WorkflowTemplate template = new WorkflowTemplate();
        template.setTemplateId(UUID.randomUUID().toString());
        template.setName(templateName);
        template.setDescription(description);
        template.setCategory(category);
        template.setVersion("1.0.0");
        template.setCreatedBy(workflow.getUserId());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setOfficial(false);
        template.setNodes(workflow.getNodes());
        template.setEdges(workflow.getEdges());
        template.setDefaultInputs(workflow.getDefaultInputs());
        template.setParameterDefinitions(parameterDefinitions);
        template.setUsageCount(0L);
        
        // Store workflow information in template metadata
        Map<String, Object> config = new HashMap<>();
        config.put("sourceWorkflowId", workflowId);
        config.put("createdAt", LocalDateTime.now());
        template.setConfig(config);
        
        // Save the template
        WorkflowTemplate savedTemplate = templateRepository.save(template);
        
        log.info("Created template: {} from workflow: {}", template.getTemplateId(), workflowId);
        
        return templateMapper.toDTO(savedTemplate);
    }

    @Override
    @Transactional
    public WorkflowTemplateDTO updateTemplate(
            String templateId,
            String category,
            String templateName,
            String description,
            Map<String, Object> parameterDefinitions) {
        
        // Get the template
        WorkflowTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTemplate", "templateId", templateId));
        
        // Update template fields
        if (templateName != null) {
            template.setName(templateName);
        }
        
        if (description != null) {
            template.setDescription(description);
        }
        
        if (category != null) {
            template.setCategory(category);
        }
        
        if (parameterDefinitions != null) {
            template.setParameterDefinitions(parameterDefinitions);
        }
        
        // Update version and timestamp
        template.setVersion(incrementVersion(template.getVersion()));
        template.setUpdateTime(LocalDateTime.now());
        
        // Save the updated template
        WorkflowTemplate updatedTemplate = templateRepository.save(template);
        
        log.info("Updated template: {}", templateId);
        
        return templateMapper.toDTO(updatedTemplate);
    }

    @Override
    @Transactional
    public void deleteTemplate(String templateId) {
        // Check if template exists
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("WorkflowTemplate", "templateId", templateId);
        }
        
        // Delete the template
        templateRepository.deleteById(templateId);
        
        log.info("Deleted template: {}", templateId);
    }

    @Override
    public List<String> getAllCategories() {
        return templateRepository.findAllCategories();
    }
    
    /**
     * Apply parameters to a template to create a customized workflow
     */
    private WorkflowDTO applyParametersToTemplate(WorkflowTemplateDTO template, Map<String, Object> parameters) {
        // Create a deep copy of the template
        WorkflowDTO workflow = templateMapper.templateToWorkflow(template);
        
        if (parameters == null || parameters.isEmpty()) {
            return workflow;
        }
        
        // Initialize variable manager with parameters
        VariableManager paramManager = variableManager;
        parameters.forEach(paramManager::setUserVariable);
        
        // Process nodes to replace parameter references
        workflow.getNodes().forEach(node -> {
            // Replace parameters in node properties
            if (node.getProperties() != null) {
                Map<String, Object> processedProps = processParameterReferences(node.getProperties(), paramManager);
                node.setProperties(processedProps);
            }
            
            // Process input configurations
            if (node.getInputs() != null) {
                node.getInputs().forEach(input -> {
                    if (input.getDefaultValue() != null) {
                        Object processedValue = processParameterReference(input.getDefaultValue(), paramManager);
                        input.setDefaultValue(processedValue);
                    }
                });
            }
        });
        
        return workflow;
    }
    
    /**
     * Process a map of values, replacing parameter references
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> processParameterReferences(Map<String, Object> values, VariableManager paramManager) {
        Map<String, Object> result = new HashMap<>();
        
        values.forEach((key, value) -> {
            if (value instanceof Map) {
                result.put(key, processParameterReferences((Map<String, Object>) value, paramManager));
            } else if (value instanceof List) {
                result.put(key, processParameterReferencesList((List<Object>) value, paramManager));
            } else {
                result.put(key, processParameterReference(value, paramManager));
            }
        });
        
        return result;
    }
    
    /**
     * Process a list of values, replacing parameter references
     */
    @SuppressWarnings("unchecked")
    private List<Object> processParameterReferencesList(List<Object> values, VariableManager paramManager) {
        List<Object> result = new ArrayList<>();
        
        values.forEach(value -> {
            if (value instanceof Map) {
                result.add(processParameterReferences((Map<String, Object>) value, paramManager));
            } else if (value instanceof List) {
                result.add(processParameterReferencesList((List<Object>) value, paramManager));
            } else {
                result.add(processParameterReference(value, paramManager));
            }
        });
        
        return result;
    }
    
    /**
     * Process a single value, replacing parameter references if it's a string
     */
    private Object processParameterReference(Object value, VariableManager paramManager) {
        if (!(value instanceof String)) {
            return value;
        }
        
        String strValue = (String) value;
        
        // Check if the value is a parameter reference (e.g., "${paramName}")
        if (strValue.startsWith("${") && strValue.endsWith("}")) {
            String paramName = strValue.substring(2, strValue.length() - 1);
            return paramManager.hasVariable(paramName) ? paramManager.getVariable(paramName) : value;
        }
        
        return value;
    }
    
    /**
     * Increment the version number (e.g., 1.0.0 -> 1.0.1)
     */
    private String incrementVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            return "1.0.0"; // Invalid version format, reset to 1.0.0
        }
        
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]) + 1;
            
            return String.format("%d.%d.%d", major, minor, patch);
        } catch (NumberFormatException e) {
            return "1.0.0"; // Invalid version format, reset to 1.0.0
        }
    }
    
    /**
     * Update the usage count of a template
     */
    private void updateTemplateUsageCount(String templateId) {
        templateRepository.incrementUsageCount(templateId);
    }
} 