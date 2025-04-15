package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.KbDataDTO;
import com.fastgpt.ai.dto.request.VectorSearchRequest;
import com.fastgpt.ai.entity.KbData;
import com.fastgpt.ai.entity.KnowledgeBase;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.mapper.KbDataMapper;
import com.fastgpt.ai.repository.KbDataRepository;
import com.fastgpt.ai.repository.KnowledgeBaseRepository;
import com.fastgpt.ai.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    private final KbDataRepository kbDataRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDataMapper kbDataMapper;
    private final RestTemplate restTemplate;
    
    // Model to dimension mapping
    private static final Map<String, Integer> MODEL_DIMENSIONS = Map.of(
        "text-embedding-ada-002", 1536,
        "text-embedding-3-small", 1536,
        "text-embedding-3-large", 3072
    );
    
    // Default to OpenAI embedding API
    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;
    
    @Value("${vector.default-model:text-embedding-ada-002}")
    private String defaultModel;

    @Override
    public List<Float> generateEmbedding(String text, String model) {
        if (model == null || model.isEmpty()) {
            model = defaultModel;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", text);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                openaiBaseUrl + "/v1/embeddings", request, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (!data.isEmpty()) {
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    return embedding.stream()
                            .map(Double::floatValue)
                            .collect(Collectors.toList());
                }
            }
            
            throw new RuntimeException("Failed to generate embedding: No embedding data in response");
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    @Override
    public double calculateSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }
        
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        // Calculate dot product
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }
        
        // Calculate cosine similarity
        double magnitude = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (magnitude == 0) {
            return 0.0;
        }
        
        return dotProduct / magnitude;
    }

    @Override
    public List<KbDataDTO> search(VectorSearchRequest request) {
        // Verify knowledge base exists
        KnowledgeBase kb = knowledgeBaseRepository.findByKbId(request.getKbId())
            .orElseThrow(() -> new ResourceNotFoundException("Knowledge Base", "kbId", request.getKbId()));
        
        // Get all kb data
        List<KbData> allData = kbDataRepository.findByKbId(request.getKbId());
        
        if (allData.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Filter by ids if specified
        if (request.getFilterIds() != null && !request.getFilterIds().isEmpty()) {
            allData = allData.stream()
                    .filter(data -> !request.getFilterIds().contains(data.getDataId()))
                    .collect(Collectors.toList());
        }
        
        // Apply metadata filters if specified
        if (request.getMetadataFilters() != null && !request.getMetadataFilters().isEmpty()) {
            allData = allData.stream()
                    .filter(data -> matchesMetadataFilters(data, request.getMetadataFilters()))
                    .collect(Collectors.toList());
        }
        
        if (request.getUseRawQuery()) {
            // For raw queries, just return the data without vector search
            return allData.stream()
                    .limit(request.getLimit())
                    .map(kbDataMapper::toDTO)
                    .collect(Collectors.toList());
        }
        
        // Generate embedding for the query
        List<Float> queryVector = generateEmbedding(request.getQuery(), kb.getVectorModel());
        
        // Calculate similarity scores
        List<ScoredKbData> scoredResults = allData.stream()
                .map(data -> {
                    double score = calculateSimilarity(queryVector, data.getVector());
                    return new ScoredKbData(data, score);
                })
                .filter(scoredData -> scoredData.getScore() >= request.getMinScore())
                .sorted(Comparator.comparingDouble(ScoredKbData::getScore).reversed())
                .limit(request.getLimit())
                .collect(Collectors.toList());
        
        // Convert to DTOs and return
        return scoredResults.stream()
                .map(scoredData -> {
                    KbDataDTO dto = kbDataMapper.toDTO(scoredData.getData());
                    dto.setScore(scoredData.getScore());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Simple approximation: 1 token ≈ 4 characters for English text
        // For more accurate counting, integrate with a tokenizer library
        return (int) Math.ceil(text.length() / 4.0);
    }
    
    /**
     * Check if a kb data entry matches the metadata filters
     */
    private boolean matchesMetadataFilters(KbData data, Map<String, List<String>> filters) {
        if (data.getCollectionMeta() == null || data.getCollectionMeta().isEmpty()) {
            return false;
        }
        
        for (Map.Entry<String, List<String>> filter : filters.entrySet()) {
            String key = filter.getKey();
            List<String> values = filter.getValue();
            
            if (!data.getCollectionMeta().containsKey(key)) {
                return false;
            }
            
            if (!values.contains(data.getCollectionMeta().get(key))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Helper class to store KB data with its score
     */
    private static class ScoredKbData {
        private final KbData data;
        private final double score;
        
        public ScoredKbData(KbData data, double score) {
            this.data = data;
            this.score = score;
        }
        
        public KbData getData() {
            return data;
        }
        
        public double getScore() {
            return score;
        }
    }
} 