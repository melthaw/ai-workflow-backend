package com.fastgpt.ai.mapper;

import com.fastgpt.ai.dto.KnowledgeBaseDTO;
import com.fastgpt.ai.dto.request.KnowledgeBaseCreateRequest;
import com.fastgpt.ai.entity.KnowledgeBase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KnowledgeBaseMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kbId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "fileCount", ignore = true)
    @Mapping(target = "kbDataCount", ignore = true)
    KnowledgeBase toEntity(KnowledgeBaseCreateRequest request);
    
    KnowledgeBaseDTO toDTO(KnowledgeBase knowledgeBase);
    
    List<KnowledgeBaseDTO> toDTOList(List<KnowledgeBase> knowledgeBases);
    
    void updateEntityFromDTO(KnowledgeBaseDTO dto, @MappingTarget KnowledgeBase knowledgeBase);
} 