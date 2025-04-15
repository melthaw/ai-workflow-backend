package com.fastgpt.ai.mapper;

import com.fastgpt.ai.dto.KbDataDTO;
import com.fastgpt.ai.dto.request.KbDataCreateRequest;
import com.fastgpt.ai.entity.KbData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KbDataMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataId", ignore = true)
    @Mapping(target = "qTokens", ignore = true)
    @Mapping(target = "aTokens", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    KbData toEntity(KbDataCreateRequest request);
    
    KbDataDTO toDTO(KbData kbData);
    
    List<KbDataDTO> toDTOList(List<KbData> kbDataList);
    
    void updateEntityFromDTO(KbDataDTO dto, @MappingTarget KbData kbData);
} 