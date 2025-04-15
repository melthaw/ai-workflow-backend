package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.KbDataDTO;
import com.fastgpt.ai.dto.KnowledgeBaseDTO;
import com.fastgpt.ai.dto.request.KbDataCreateRequest;
import com.fastgpt.ai.dto.request.KnowledgeBaseCreateRequest;
import com.fastgpt.ai.dto.request.VectorSearchRequest;
import com.fastgpt.ai.dto.response.ApiResponse;
import com.fastgpt.ai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    // Knowledge Base endpoints
    
    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeBaseDTO>> createKnowledgeBase(
            @Valid @RequestBody KnowledgeBaseCreateRequest request) {
        log.info("Creating knowledge base for user: {}", request.getUserId());
        KnowledgeBaseDTO kb = knowledgeBaseService.createKnowledgeBase(request);
        return ResponseEntity.ok(ApiResponse.success(kb));
    }
    
    @GetMapping("/{kbId}")
    public ResponseEntity<ApiResponse<KnowledgeBaseDTO>> getKnowledgeBase(@PathVariable String kbId) {
        log.info("Getting knowledge base with ID: {}", kbId);
        KnowledgeBaseDTO kb = knowledgeBaseService.getKnowledgeBaseById(kbId);
        return ResponseEntity.ok(ApiResponse.success(kb));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseDTO>>> getKnowledgeBasesByUser(@PathVariable String userId) {
        log.info("Getting knowledge bases for user: {}", userId);
        List<KnowledgeBaseDTO> kbs = knowledgeBaseService.getKnowledgeBasesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(kbs));
    }
    
    @GetMapping("/accessible/{userId}")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseDTO>>> getAccessibleKnowledgeBases(@PathVariable String userId) {
        log.info("Getting accessible knowledge bases for user: {}", userId);
        List<KnowledgeBaseDTO> kbs = knowledgeBaseService.getAccessibleKnowledgeBases(userId);
        return ResponseEntity.ok(ApiResponse.success(kbs));
    }
    
    @PutMapping("/{kbId}")
    public ResponseEntity<ApiResponse<KnowledgeBaseDTO>> updateKnowledgeBase(
            @PathVariable String kbId,
            @RequestBody KnowledgeBaseDTO update) {
        log.info("Updating knowledge base with ID: {}", kbId);
        KnowledgeBaseDTO kb = knowledgeBaseService.updateKnowledgeBase(kbId, update);
        return ResponseEntity.ok(ApiResponse.success(kb));
    }
    
    @DeleteMapping("/{kbId}")
    public ResponseEntity<ApiResponse<Void>> deleteKnowledgeBase(@PathVariable String kbId) {
        log.info("Deleting knowledge base with ID: {}", kbId);
        knowledgeBaseService.deleteKnowledgeBase(kbId);
        return ResponseEntity.ok(ApiResponse.success("Knowledge base deleted successfully", null));
    }
    
    // KB Data endpoints
    
    @PostMapping("/data")
    public ResponseEntity<ApiResponse<KbDataDTO>> addData(@Valid @RequestBody KbDataCreateRequest request) {
        log.info("Adding data to knowledge base: {}", request.getKbId());
        KbDataDTO data = knowledgeBaseService.addData(request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    @GetMapping("/data/{dataId}")
    public ResponseEntity<ApiResponse<KbDataDTO>> getData(@PathVariable String dataId) {
        log.info("Getting KB data with ID: {}", dataId);
        KbDataDTO data = knowledgeBaseService.getDataById(dataId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    @GetMapping("/{kbId}/data")
    public ResponseEntity<ApiResponse<List<KbDataDTO>>> getKbData(@PathVariable String kbId) {
        log.info("Getting all data for knowledge base: {}", kbId);
        List<KbDataDTO> data = knowledgeBaseService.getDataByKbId(kbId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    @DeleteMapping("/data/{dataId}")
    public ResponseEntity<ApiResponse<Void>> deleteData(@PathVariable String dataId) {
        log.info("Deleting KB data with ID: {}", dataId);
        knowledgeBaseService.deleteData(dataId);
        return ResponseEntity.ok(ApiResponse.success("KB data deleted successfully", null));
    }
    
    // Vector search endpoint
    
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<KbDataDTO>>> search(@RequestBody VectorSearchRequest request) {
        log.info("Searching knowledge base: {}", request.getKbId());
        List<KbDataDTO> results = knowledgeBaseService.search(request);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
} 