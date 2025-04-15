package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.AppDTO;
import com.fastgpt.ai.dto.request.AppCreateRequest;
import com.fastgpt.ai.dto.request.AppUpdateRequest;
import com.fastgpt.ai.dto.request.KbConfigUpdateRequest;
import com.fastgpt.ai.entity.App;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.mapper.AppMapper;
import com.fastgpt.ai.repository.AppRepository;
import com.fastgpt.ai.service.AppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {

    private final AppRepository appRepository;
    private final AppMapper appMapper;

    @Override
    @Transactional
    public AppDTO createApp(AppCreateRequest request) {
        App app = appMapper.toEntity(request);
        
        // Generate a unique appId
        app.setAppId(UUID.randomUUID().toString());
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        app.setCreateTime(now);
        app.setUpdateTime(now);
        
        App savedApp = appRepository.save(app);
        
        return appMapper.toDTO(savedApp);
    }

    @Override
    public AppDTO getAppById(String appId) {
        return appRepository.findByAppId(appId)
                .map(appMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("App", "appId", appId));
    }

    @Override
    public List<AppDTO> getAppsByUserId(String userId) {
        return appMapper.toDTOList(appRepository.findByUserId(userId));
    }

    @Override
    public List<AppDTO> getAppsByTeamId(String teamId) {
        return appMapper.toDTOList(appRepository.findByTeamId(teamId));
    }

    @Override
    public List<AppDTO> getAccessibleApps(String userId, String teamId) {
        return appMapper.toDTOList(appRepository.findByUserIdOrTeamId(userId, teamId));
    }

    @Override
    @Transactional
    public AppDTO updateApp(AppUpdateRequest request) {
        App app = appRepository.findByAppId(request.getAppId())
                .orElseThrow(() -> new ResourceNotFoundException("App", "appId", request.getAppId()));
        
        appMapper.updateEntityFromDTO(request, app);
        
        // Update timestamp
        app.setUpdateTime(LocalDateTime.now());
        
        App updatedApp = appRepository.save(app);
        
        return appMapper.toDTO(updatedApp);
    }
    
    @Override
    @Transactional
    public AppDTO updateKbConfig(KbConfigUpdateRequest request) {
        App app = appRepository.findByAppId(request.getAppId())
                .orElseThrow(() -> new ResourceNotFoundException("App", "appId", request.getAppId()));
        
        // Update knowledge base configuration
        app.setUseKb(request.getUseKb());
        app.setKbIds(request.getKbIds());
        app.setRagConfig(request.getRagConfig());
        
        // Update timestamp
        app.setUpdateTime(LocalDateTime.now());
        
        App updatedApp = appRepository.save(app);
        
        return appMapper.toDTO(updatedApp);
    }

    @Override
    @Transactional
    public void deleteApp(String appId) {
        if (!appRepository.findByAppId(appId).isPresent()) {
            throw new ResourceNotFoundException("App", "appId", appId);
        }
        
        appRepository.deleteByAppId(appId);
    }
} 