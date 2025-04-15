package com.fastgpt.ai.mapper;

import com.fastgpt.ai.dto.ChatDTO;
import com.fastgpt.ai.dto.request.ChatCreateRequest;
import com.fastgpt.ai.entity.Chat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "chatId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "shareId", ignore = true)
    @Mapping(target = "outLinkUid", ignore = true)
    @Mapping(target = "top", ignore = true)
    Chat toEntity(ChatCreateRequest request);
    
    ChatDTO toDTO(Chat chat);
    
    List<ChatDTO> toDTOList(List<Chat> chats);
    
    void updateEntityFromDTO(ChatDTO dto, @MappingTarget Chat chat);
} 