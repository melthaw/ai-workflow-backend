package com.fastgpt.ai.mapper;

import com.fastgpt.ai.dto.ChatItemValueDTO;
import com.fastgpt.ai.entity.ChatItemValue;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for converting between ChatItemValue entity and ChatItemValueDTO
 */
@Mapper
public interface ChatItemValueMapper {
    
    ChatItemValueMapper INSTANCE = Mappers.getMapper(ChatItemValueMapper.class);
    
    /**
     * Convert entity to DTO
     * 
     * @param entity the ChatItemValue entity
     * @return the ChatItemValueDTO
     */
    ChatItemValueDTO toDTO(ChatItemValue entity);
    
    /**
     * Convert DTO to entity
     * 
     * @param dto the ChatItemValueDTO
     * @return the ChatItemValue entity
     */
    ChatItemValue toEntity(ChatItemValueDTO dto);
} 