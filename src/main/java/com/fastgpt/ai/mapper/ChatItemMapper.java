package com.fastgpt.ai.mapper;

import com.fastgpt.ai.dto.ChatItemDTO;
import com.fastgpt.ai.entity.ChatItem;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatItemMapper {
    
    ChatItemDTO toDTO(ChatItem chatItem);
    
    List<ChatItemDTO> toDTOList(List<ChatItem> chatItems);
    
    void updateEntityFromDTO(ChatItemDTO dto, @MappingTarget ChatItem chatItem);
} 