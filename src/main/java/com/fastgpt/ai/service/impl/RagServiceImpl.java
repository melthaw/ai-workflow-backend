package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.KbDataDTO;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.RagService;
import com.fastgpt.ai.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of Retrieval Augmented Generation (RAG) service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    
    private final VectorService vectorService;
    private final AiService aiService;

    @Override
    public Map<String, Object> getRagResponse(String query, List<String> kbIds, Map<String, Object> extraParams) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Starting RAG process for query: {}, kbIds: {}", query, kbIds);
            
            // Get parameters or use defaults
            int limit = extraParams.containsKey("limit") ? (Integer) extraParams.get("limit") : 5;
            double minScore = extraParams.containsKey("minScore") ? (Double) extraParams.get("minScore") : 0.7;
            String systemPrompt = (String) extraParams.getOrDefault("systemPrompt", "");
            
            // Retrieve context from knowledge bases
            List<KbDataDTO> retrievedDocs = retrieveContext(query, kbIds, limit, minScore);
            
            // Format the context for inclusion in the prompt
            String formattedContext = formatRetrievedContext(retrievedDocs);
            
            // Create the complete prompt with context
            String fullPrompt = createRagPrompt(query, formattedContext, systemPrompt);
            
            // Generate answer using AI service
            String answer = aiService.generateText(fullPrompt);
            
            // Prepare result with answer and sources
            result.put("answer", answer);
            result.put("sources", retrievedDocs);
            result.put("executionTime", System.currentTimeMillis() - startTime);
            
            log.info("RAG process completed in {}ms", System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("Error in RAG process: {}", e.getMessage(), e);
            result.put("answer", "I encountered an error while searching knowledge bases: " + e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Perform retrieval to find relevant context for a query
     */
    private List<KbDataDTO> retrieveContext(String query, List<String> kbIds, int limit, double minScore) {
        List<KbDataDTO> allResults = new ArrayList<>();
        
        for (String kbId : kbIds) {
            try {
                log.info("Executing vector search for kbId: {}", kbId);
                
                // Prepare vector search request
                Map<String, Object> vectorSearchRequest = new HashMap<>();
                vectorSearchRequest.put("query", query);
                vectorSearchRequest.put("kbId", kbId);
                vectorSearchRequest.put("limit", limit);
                vectorSearchRequest.put("minScore", minScore);
                
                // Execute vector search
                List<KbDataDTO> kbResults = vectorService.search(vectorSearchRequest);
                allResults.addAll(kbResults);
                
                log.info("Retrieved {} results from kbId: {}", kbResults.size(), kbId);
            } catch (Exception e) {
                log.error("Error searching kbId {}: {}", kbId, e.getMessage(), e);
            }
        }
        
        // Merge and rank all results
        return mergeAndRankResults(allResults, limit, minScore);
    }
    
    /**
     * Merge and rank results from multiple knowledge bases
     */
    private List<KbDataDTO> mergeAndRankResults(List<KbDataDTO> allResults, int limit, double minScore) {
        // Sort by score (highest first)
        allResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // Filter by minimum score and limit results
        return allResults.stream()
                .filter(doc -> doc.getScore() >= minScore)
                .limit(limit)
                .toList();
    }
    
    /**
     * Format retrieved context for prompt construction
     */
    private String formatRetrievedContext(List<KbDataDTO> retrievedDocs) {
        if (retrievedDocs.isEmpty()) {
            return "No relevant information found.";
        }
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Context information:\n\n");
        
        for (int i = 0; i < retrievedDocs.size(); i++) {
            KbDataDTO doc = retrievedDocs.get(i);
            contextBuilder.append("---\n");
            contextBuilder.append("Source ").append(i + 1).append(": ");
            if (doc.getMetadata() != null && doc.getMetadata().containsKey("source")) {
                contextBuilder.append(doc.getMetadata().get("source"));
            }
            contextBuilder.append("\n");
            contextBuilder.append(doc.getQ()).append("\n");
            contextBuilder.append(doc.getA()).append("\n");
            contextBuilder.append("---\n\n");
        }
        
        return contextBuilder.toString();
    }
    
    /**
     * Create a RAG prompt by combining user query with retrieved context
     */
    private String createRagPrompt(String query, String context, String systemPrompt) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Add system prompt if provided
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            promptBuilder.append(systemPrompt).append("\n\n");
        } else {
            // Default system prompt
            promptBuilder.append("You are a helpful assistant that answers questions based on the provided context. ");
            promptBuilder.append("If the context doesn't contain relevant information to answer the question, ");
            promptBuilder.append("acknowledge that you don't know rather than making up an answer.\n\n");
        }
        
        // Add context
        promptBuilder.append(context).append("\n\n");
        
        // Add user query
        promptBuilder.append("Question: ").append(query).append("\n");
        promptBuilder.append("Answer: ");
        
        return promptBuilder.toString();
    }
} 