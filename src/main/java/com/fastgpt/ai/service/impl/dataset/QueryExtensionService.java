package com.fastgpt.ai.service.impl.dataset;

import com.fastgpt.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Service for extending user queries to improve RAG results
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExtensionService {

    private final AiService aiService;
    
    // Prompt templates
    private static final String QUERY_EXTENSION_PROMPT = 
        "As an AI assistant, your task is to expand the user's search query to improve retrieval performance.\n" +
        "For the given query, please generate 2-3 alternative phrasings or related questions that cover different aspects of the original query.\n" +
        "The goal is to increase the chance of finding relevant information by covering different ways to express the same information need.\n\n" +
        "Original query: \"%s\"\n\n" +
        "Output only the alternative queries, one per line. Do not include numbering, explanations, or any other text.";
    
    /**
     * Generate extended queries for a given user query
     * 
     * @param query The original user query
     * @param model The LLM model to use for extension
     * @param maxResults Maximum number of extended queries to return
     * @return List of extended queries including the original
     */
    public List<String> extendQuery(String query, String model, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Create the prompt
            String prompt = String.format(QUERY_EXTENSION_PROMPT, query);
            
            // Execute query extension with timeout
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 
                aiService.generateSimpleResponse(prompt, null)
            );
            
            String result = future.get(10, TimeUnit.SECONDS);
            
            // Process and filter results
            List<String> extendedQueries = new ArrayList<>();
            extendedQueries.add(query); // Always include the original query
            
            // Add expanded queries if available
            if (result != null && !result.isEmpty()) {
                Arrays.stream(result.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.equals(query)) // Avoid duplication
                    .limit(maxResults - 1) // Limit to maxResults - 1 (to account for original)
                    .forEach(extendedQueries::add);
            }
            
            log.info("Extended query '{}' into {} queries", query, extendedQueries.size());
            return extendedQueries;
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error extending query: {}", e.getMessage(), e);
            return Collections.singletonList(query); // Fallback to original query
        }
    }
    
    /**
     * Get the tokens used for query extension
     * This is an estimation for billing purposes
     * 
     * @param query The original query
     * @param result The query extension result
     * @return TokenUsage containing input and output token counts
     */
    public TokenUsage estimateTokenUsage(String query, String result) {
        // This is a simplified estimation - a more accurate implementation would use a proper tokenizer
        int inputTokens = (QUERY_EXTENSION_PROMPT.length() + query.length()) / 4; // Rough estimate: 4 chars ≈ 1 token
        int outputTokens = result != null ? result.length() / 4 : 0;
        
        return new TokenUsage(inputTokens, outputTokens);
    }
    
    /**
     * Record class for token usage
     */
    public record TokenUsage(int inputTokens, int outputTokens) {}
} 