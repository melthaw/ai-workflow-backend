package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.response.ApiResponse;
import com.fastgpt.ai.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryRag(
            @RequestBody Map<String, Object> request) {
        
        String query = (String) request.get("query");
        @SuppressWarnings("unchecked")
        List<String> kbIds = (List<String>) request.get("kbIds");
        @SuppressWarnings("unchecked")
        Map<String, Object> extraParams = (Map<String, Object>) request.get("extraParams");
        
        log.info("RAG query: {}, KBs: {}", query, kbIds);
        
        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Query cannot be empty"));
        }
        
        if (kbIds == null || kbIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "At least one knowledge base ID must be provided"));
        }
        
        Map<String, Object> result = ragService.getRagResponse(query, kbIds, extraParams);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
} 