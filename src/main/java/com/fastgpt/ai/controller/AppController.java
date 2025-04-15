package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.AppDTO;
import com.fastgpt.ai.dto.request.AppCreateRequest;
import com.fastgpt.ai.dto.request.AppUpdateRequest;
import com.fastgpt.ai.dto.request.KbConfigUpdateRequest;
import com.fastgpt.ai.dto.response.ApiResponse;
import com.fastgpt.ai.service.AppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/apps")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @PostMapping
    public ResponseEntity<ApiResponse<AppDTO>> createApp(@Valid @RequestBody AppCreateRequest request) {
        log.info("Creating app for user: {}", request.getUserId());
        AppDTO createdApp = appService.createApp(request);
        return ResponseEntity.ok(ApiResponse.success(createdApp));
    }

    @GetMapping("/{appId}")
    public ResponseEntity<ApiResponse<AppDTO>> getAppById(@PathVariable String appId) {
        log.info("Getting app with ID: {}", appId);
        AppDTO app = appService.getAppById(appId);
        return ResponseEntity.ok(ApiResponse.success(app));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AppDTO>>> getAppsByUserId(@PathVariable String userId) {
        log.info("Getting all apps for user: {}", userId);
        List<AppDTO> apps = appService.getAppsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<AppDTO>>> getAppsByTeamId(@PathVariable String teamId) {
        log.info("Getting all apps for team: {}", teamId);
        List<AppDTO> apps = appService.getAppsByTeamId(teamId);
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/accessible")
    public ResponseEntity<ApiResponse<List<AppDTO>>> getAccessibleApps(
            @RequestParam String userId,
            @RequestParam(required = false) String teamId) {
        log.info("Getting accessible apps for user: {} and team: {}", userId, teamId);
        List<AppDTO> apps = appService.getAccessibleApps(userId, teamId);
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AppDTO>> updateApp(@Valid @RequestBody AppUpdateRequest request) {
        log.info("Updating app with ID: {}", request.getAppId());
        AppDTO updatedApp = appService.updateApp(request);
        return ResponseEntity.ok(ApiResponse.success(updatedApp));
    }
    
    @PutMapping("/{appId}/kb-config")
    public ResponseEntity<ApiResponse<AppDTO>> updateKbConfig(
            @PathVariable String appId,
            @Valid @RequestBody KbConfigUpdateRequest request) {
        log.info("Updating knowledge base config for app: {}", appId);
        request.setAppId(appId); // Ensure appId is set
        AppDTO updatedApp = appService.updateKbConfig(request);
        return ResponseEntity.ok(ApiResponse.success(updatedApp));
    }

    @DeleteMapping("/{appId}")
    public ResponseEntity<ApiResponse<Void>> deleteApp(@PathVariable String appId) {
        log.info("Deleting app with ID: {}", appId);
        appService.deleteApp(appId);
        return ResponseEntity.ok(ApiResponse.success("App deleted successfully", null));
    }
} 