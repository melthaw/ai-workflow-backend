package com.fastgpt.ai.mapper;

import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.workflow.WorkflowTemplateDTO;
import com.fastgpt.ai.entity.WorkflowTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper for converting between WorkflowTemplate entity and DTO
 */
@Mapper(componentModel = "spring")
public interface WorkflowTemplateMapper {
    
    WorkflowTemplateMapper INSTANCE = Mappers.getMapper(WorkflowTemplateMapper.class);
    
    /**
     * Convert entity to DTO
     * @param template Entity
     * @return DTO
     */
    WorkflowTemplateDTO toDTO(WorkflowTemplate template);
    
    /**
     * Convert DTO to entity
     * @param dto DTO
     * @return Entity
     */
    WorkflowTemplate toEntity(WorkflowTemplateDTO dto);
    
    /**
     * Convert template DTO to workflow DTO
     * @param template Template DTO
     * @return Workflow DTO
     */
    @Mapping(target = "workflowId", ignore = true)
    @Mapping(target = "userId", source = "createdBy")
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "appId", ignore = true)
    @Mapping(target = "moduleId", ignore = true)
    @Mapping(target = "status", constant = "draft")
    WorkflowDTO templateToWorkflow(WorkflowTemplateDTO template);
    
    /**
     * Update entity from DTO
     * @param dto Source DTO
     * @param template Target entity
     */
    void updateEntityFromDTO(WorkflowTemplateDTO dto, @MappingTarget WorkflowTemplate template);
    
    /**
     * Convert list of entities to list of DTOs
     * @param templates List of entities
     * @return List of DTOs
     */
    List<WorkflowTemplateDTO> toDTOList(List<WorkflowTemplate> templates);
} 