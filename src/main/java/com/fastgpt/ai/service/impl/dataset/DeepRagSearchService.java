package com.fastgpt.ai.service.impl.dataset;

import com.fastgpt.ai.dto.SearchDataResponseItemDTO;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for deep RAG searches that involve iterative refinement
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepRagSearchService {

    private final AiService aiService;
    private final VectorService vectorService;
    private final QueryExtensionService queryExtensionService;
    
    // Prompts for deep search
    private static final String DEEP_SEARCH_PROMPT_TEMPLATE = 
        "Based on my original question and the search results so far, I need you to formulate a follow-up question that will help me find more comprehensive information.\n\n" +
        "Original Question: %s\n\n" +
        "Search Results So Far:\n%s\n\n" +
        "Generate a specific follow-up question that addresses gaps in the current results or explores aspects not yet covered. " +
        "Be precise and focus on retrieving additional relevant information.\n\n" +
        "Follow-up Question:";
    
    /**
     * Result class for deep RAG search
     */
    public record DeepSearchResult(
        List<SearchDataResponseItemDTO> results,
        List<String> generatedQueries,
        int totalQueries,
        int inputTokens,
        int outputTokens,
        String model
    ) {}
    
    /**
     * Perform a deep RAG search using iterative refinement
     * 
     * @param originalQuery The user's original query
     * @param initialResults Initial search results
     * @param model LLM model to use for generating follow-up questions
     * @param maxIterations Maximum number of search iterations
     * @param minResults Minimum number of results needed before stopping
     * @param datasetIds Dataset IDs to search
     * @param similarityThreshold Minimum similarity score for results
     * @return DeepSearchResult containing results and metadata
     */
    public DeepSearchResult performDeepSearch(
            String originalQuery,
            List<SearchDataResponseItemDTO> initialResults,
            String model,
            int maxIterations,
            int minResults,
            List<String> datasetIds,
            double similarityThreshold) {
        
        if (originalQuery == null || originalQuery.isEmpty()) {
            return new DeepSearchResult(
                Collections.emptyList(), 
                Collections.emptyList(),
                0, 0, 0, model
            );
        }
        
        // Track metrics
        AtomicInteger inputTokens = new AtomicInteger(0);
        AtomicInteger outputTokens = new AtomicInteger(0);
        List<String> generatedQueries = new ArrayList<>();
        
        // Start with initial results and original query
        Set<String> uniqueResultIds = new HashSet<>();
        List<SearchDataResponseItemDTO> allResults = new ArrayList<>(initialResults);
        
        // Add initial results to unique set
        initialResults.forEach(result -> uniqueResultIds.add(result.getId()));
        
        // If we already have enough results, return early
        if (allResults.size() >= minResults) {
            return new DeepSearchResult(
                allResults,
                generatedQueries,
                1,
                inputTokens.get(),
                outputTokens.get(),
                model
            );
        }
        
        // Perform iterative search
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Generate follow-up question based on results so far
            String followUpQuestion = generateFollowUpQuestion(
                originalQuery, 
                allResults, 
                model,
                inputTokens,
                outputTokens
            );
            
            if (followUpQuestion == null || followUpQuestion.isEmpty()) {
                break;
            }
            
            // Add to generated queries list
            generatedQueries.add(followUpQuestion);
            
            // Perform vector search with the follow-up question
            List<SearchDataResponseItemDTO> iterationResults = vectorService.searchSimilarVectors(
                followUpQuestion,
                datasetIds,
                similarityThreshold
            );
            
            // Filter out results we've already seen
            List<SearchDataResponseItemDTO> newResults = iterationResults.stream()
                .filter(result -> !uniqueResultIds.contains(result.getId()))
                .collect(Collectors.toList());
            
            // If we found new results, add them
            if (!newResults.isEmpty()) {
                newResults.forEach(result -> {
                    uniqueResultIds.add(result.getId());
                    allResults.add(result);
                });
                
                log.info("Deep search iteration {}: found {} new results", 
                        iteration + 1, newResults.size());
            } else {
                log.info("Deep search iteration {}: no new results found", iteration + 1);
            }
            
            // If we have enough results or didn't find anything new, stop
            if (allResults.size() >= minResults || newResults.isEmpty()) {
                break;
            }
        }
        
        // Return all results with metadata
        return new DeepSearchResult(
            allResults,
            generatedQueries,
            generatedQueries.size() + 1, // Include original query
            inputTokens.get(),
            outputTokens.get(),
            model
        );
    }
    
    /**
     * Generate a follow-up question based on existing results
     */
    private String generateFollowUpQuestion(
            String originalQuery, 
            List<SearchDataResponseItemDTO> results,
            String model,
            AtomicInteger inputTokens,
            AtomicInteger outputTokens) {
        
        // Format existing results as string
        String resultsText = results.stream()
            .limit(3) // Use at most 3 results to avoid token limits
            .map(result -> String.format("- %s\n%s", result.getQ(), result.getA()))
            .collect(Collectors.joining("\n\n"));
        
        // Create prompt
        String prompt = String.format(DEEP_SEARCH_PROMPT_TEMPLATE, originalQuery, resultsText);
        
        try {
            // Generate follow-up question
            String followUpQuestion = aiService.generateSimpleResponse(prompt, null);
            
            // Update token counts (estimate)
            inputTokens.addAndGet(prompt.length() / 4); // Very rough estimation
            outputTokens.addAndGet(followUpQuestion != null ? followUpQuestion.length() / 4 : 0);
            
            // Clean up and return the question
            return followUpQuestion != null 
                ? followUpQuestion.trim().replaceAll("^[\"']|[\"']$", "") 
                : null;
            
        } catch (Exception e) {
            log.error("Error generating follow-up question: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Merge and rank results from multiple searches (original + deep searches)
     */
    public List<SearchDataResponseItemDTO> mergeAndRankResults(List<List<SearchDataResponseItemDTO>> resultSets) {
        // Use a map to deduplicate by ID while preserving the highest score
        Map<String, SearchDataResponseItemDTO> mergedResults = new HashMap<>();
        
        // Process each result set (with priority to earlier sets)
        int setIndex = 0;
        for (List<SearchDataResponseItemDTO> resultSet : resultSets) {
            // Apply a slight boost based on result set position (0.05 per position, max 0.2)
            double boost = Math.min(0.2, 0.05 * (resultSets.size() - setIndex - 1));
            
            for (SearchDataResponseItemDTO result : resultSet) {
                String resultId = result.getId();
                
                // If we already have this result, keep the one with the higher score
                if (mergedResults.containsKey(resultId)) {
                    SearchDataResponseItemDTO existing = mergedResults.get(resultId);
                    double boostedScore = result.getScore() + boost;
                    
                    if (boostedScore > existing.getScore()) {
                        // Create a copy with the higher score
                        SearchDataResponseItemDTO updated = copyWithScore(result, boostedScore);
                        mergedResults.put(resultId, updated);
                    }
                } else {
                    // Add new result with boosted score
                    double boostedScore = result.getScore() + boost;
                    SearchDataResponseItemDTO boosted = copyWithScore(result, boostedScore);
                    mergedResults.put(resultId, boosted);
                }
            }
            
            setIndex++;
        }
        
        // Sort by score (descending) and return as list
        return mergedResults.values().stream()
            .sorted(Comparator.comparing(SearchDataResponseItemDTO::getScore).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Create a copy of a search result with a new score
     */
    private SearchDataResponseItemDTO copyWithScore(SearchDataResponseItemDTO original, double newScore) {
        SearchDataResponseItemDTO copy = new SearchDataResponseItemDTO();
        
        // Copy all fields
        copy.setId(original.getId());
        copy.setQ(original.getQ());
        copy.setA(original.getA());
        copy.setFileId(original.getFileId());
        copy.setSourceName(original.getSourceName());
        copy.setUpdateTime(original.getUpdateTime());
        
        // Set new score
        copy.setScore(newScore);
        
        return copy;
    }
} 