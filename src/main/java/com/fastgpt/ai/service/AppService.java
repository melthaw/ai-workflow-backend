package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.AppDTO;
import com.fastgpt.ai.dto.request.AppCreateRequest;
import com.fastgpt.ai.dto.request.AppUpdateRequest;
import com.fastgpt.ai.dto.request.KbConfigUpdateRequest;

import java.util.List;

public interface AppService {
    
    /**
     * Create a new app
     * @param request App creation request
     * @return The created app DTO
     */
    AppDTO createApp(AppCreateRequest request);
    
    /**
     * Get an app by ID
     * @param appId App ID
     * @return App DTO
     */
    AppDTO getAppById(String appId);
    
    /**
     * Get all apps by user ID
     * @param userId User ID
     * @return List of app DTOs
     */
    List<AppDTO> getAppsByUserId(String userId);
    
    /**
     * Get all apps by team ID
     * @param teamId Team ID
     * @return List of app DTOs
     */
    List<AppDTO> getAppsByTeamId(String teamId);
    
    /**
     * Get all apps accessible by user (personal and team apps)
     * @param userId User ID
     * @param teamId Team ID
     * @return List of app DTOs
     */
    List<AppDTO> getAccessibleApps(String userId, String teamId);
    
    /**
     * Update an app
     * @param request App update request
     * @return The updated app DTO
     */
    AppDTO updateApp(AppUpdateRequest request);
    
    /**
     * Update knowledge base configuration for an app
     * @param request Knowledge base configuration update request
     * @return The updated app DTO
     */
    AppDTO updateKbConfig(KbConfigUpdateRequest request);
    
    /**
     * Delete an app by ID
     * @param appId App ID
     */
    void deleteApp(String appId);
} 