package com.fastgpt.ai.service.impl.workflow.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Tool provider for web search functionality
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchToolProvider implements ToolProvider {

    private final RestTemplate restTemplate;
    
    @Value("${tools.search.enabled:false}")
    private boolean searchEnabled;
    
    @Value("${tools.search.api-key:}")
    private String searchApiKey;
    
    @Value("${tools.search.engine-id:}")
    private String searchEngineId;
    
    @Override
    public void registerTools(ToolRegistryService registry) {
        if (!searchEnabled) {
            log.info("Web search tools are disabled");
            return;
        }
        
        if (searchApiKey.isEmpty() || searchEngineId.isEmpty()) {
            log.warn("Web search tools are enabled but API key or engine ID is missing");
            return;
        }
        
        // Register web search tool
        registry.registerTool(
            "web_search",
            "Search the web for information",
            createWebSearchParameterSchema(),
            this::executeWebSearch
        );
        
        log.info("Registered web search tools");
    }
    
    @Override
    public String getCategory() {
        return "web";
    }
    
    /**
     * Create parameter schema for web search tool
     */
    private Map<String, Object> createWebSearchParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("query"));
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> queryProp = new HashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "The search query");
        properties.put("query", queryProp);
        
        Map<String, Object> numResultsProp = new HashMap<>();
        numResultsProp.put("type", "integer");
        numResultsProp.put("description", "Number of search results to return");
        numResultsProp.put("default", 5);
        numResultsProp.put("minimum", 1);
        numResultsProp.put("maximum", 10);
        properties.put("numResults", numResultsProp);
        
        schema.put("properties", properties);
        
        return schema;
    }
    
    /**
     * Execute web search with given parameters
     */
    private Object executeWebSearch(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        int numResults = parameters.containsKey("numResults") 
                ? ((Number) parameters.get("numResults")).intValue() 
                : 5;
        
        try {
            // Encode the query
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            // Build the URL
            String url = String.format(
                "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=%d",
                searchApiKey, searchEngineId, encodedQuery, numResults
            );
            
            // Make the request
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Search API returned error: " + response.getStatusCode());
            }
            
            // Extract search results
            Map<String, Object> responseBody = response.getBody();
            
            // Process search results
            List<Map<String, Object>> searchResults = new ArrayList<>();
            
            if (responseBody != null && responseBody.containsKey("items")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) responseBody.get("items");
                
                for (Map<String, Object> item : items) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("title", item.get("title"));
                    result.put("link", item.get("link"));
                    result.put("snippet", item.get("snippet"));
                    searchResults.add(result);
                }
            }
            
            // Create response
            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("results", searchResults);
            result.put("totalResults", searchResults.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error executing web search: {}", e.getMessage(), e);
            
            // Return error response
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("query", query);
            errorResult.put("error", e.getMessage());
            errorResult.put("results", List.of());
            errorResult.put("totalResults", 0);
            
            return errorResult;
        }
    }
} 