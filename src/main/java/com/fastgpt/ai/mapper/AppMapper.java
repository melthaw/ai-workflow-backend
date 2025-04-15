package com.fastgpt.ai.mapper;

import com.fastgpt.ai.dto.AppDTO;
import com.fastgpt.ai.dto.request.AppCreateRequest;
import com.fastgpt.ai.dto.request.AppUpdateRequest;
import com.fastgpt.ai.entity.App;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "appId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    App toEntity(AppCreateRequest request);
    
    AppDTO toDTO(App app);
    
    List<AppDTO> toDTOList(List<App> apps);
    
    void updateEntityFromDTO(AppUpdateRequest request, @MappingTarget App app);
} 