package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.NodeDispatcher;
import com.fastgpt.ai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Node dispatcher for retrieving information from knowledge bases
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseRetrievalNodeDispatcher implements NodeDispatcher {

    private final Optional<KnowledgeBaseService> knowledgeBaseService;
    
    private static final String NODE_TYPE = "ai.knowledgebase.retrieval";
    
    // Default parameters
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    private static final String DEFAULT_RETRIEVE_STRATEGY = "simple";
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Ensure knowledge base service is available
            if (knowledgeBaseService.isEmpty()) {
                return NodeOutDTO.error("Knowledge base service is not available");
            }
            
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return retrieveFromKnowledgeBase(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in knowledge base retrieval node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Knowledge base retrieval failed: " + e.getMessage());
        }
    }
    
    /**
     * Process knowledge base retrieval based on inputs
     */
    private NodeOutDTO retrieveFromKnowledgeBase(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing knowledge base retrieval node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get configuration parameters
            int maxResults = getIntParam(nodeData, "maxResults", DEFAULT_MAX_RESULTS);
            double similarityThreshold = getDoubleParam(nodeData, "similarityThreshold", DEFAULT_SIMILARITY_THRESHOLD);
            String retrieveStrategy = getStringParam(nodeData, "retrieveStrategy", DEFAULT_RETRIEVE_STRATEGY);
            String knowledgeBaseId = getStringParam(nodeData, "knowledgeBaseId", "");
            
            // Get filters if any
            @SuppressWarnings("unchecked")
            Map<String, Object> filters = nodeData.get("filters") instanceof Map 
                ? (Map<String, Object>) nodeData.get("filters") 
                : new HashMap<>();
            
            // Get query text
            String query = getStringParam(inputs, "query", "");
            if (query.isEmpty()) {
                return NodeOutDTO.error("No query provided for knowledge base search");
            }
            
            // Build search parameters
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("query", query);
            searchParams.put("maxResults", maxResults);
            searchParams.put("similarityThreshold", similarityThreshold);
            searchParams.put("retrieveStrategy", retrieveStrategy);
            
            if (!knowledgeBaseId.isEmpty()) {
                searchParams.put("knowledgeBaseId", knowledgeBaseId);
            }
            
            if (!filters.isEmpty()) {
                searchParams.put("filters", filters);
            }
            
            // Retrieve documents
            List<Document> documents = new ArrayList<>();
            try {
                // Use knowledge base service
                documents = knowledgeBaseService.get().searchDocuments(searchParams);
            } catch (Exception e) {
                log.error("Error performing knowledge base search: {}", e.getMessage());
                return NodeOutDTO.error("Knowledge base search failed: " + e.getMessage());
            }
            
            // Filter by similarity threshold if applicable
            if (similarityThreshold > 0) {
                documents = documents.stream()
                    .filter(doc -> {
                        // Check if score is present in metadata
                        Object scoreObj = doc.getMetadata().get("score");
                        if (scoreObj instanceof Number) {
                            double score = ((Number) scoreObj).doubleValue();
                            return score >= similarityThreshold;
                        }
                        return true; // If no score, keep the document
                    })
                    .collect(Collectors.toList());
            }
            
            // Process results
            List<Map<String, Object>> results = new ArrayList<>();
            for (Document doc : documents) {
                Map<String, Object> result = new HashMap<>();
                result.put("content", doc.getContent());
                result.put("metadata", doc.getMetadata());
                result.put("score", doc.getMetadata().getOrDefault("score", 0.0));
                results.add(result);
            }
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("results", results);
            outputs.put("count", results.size());
            
            // For convenience, extract just the text content
            List<String> contentList = new ArrayList<>();
            for (Document doc : documents) {
                contentList.add(doc.getContent());
            }
            outputs.put("contentList", contentList);
            
            // Concatenate results if needed
            if (!results.isEmpty()) {
                StringBuilder combinedContent = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> result = results.get(i);
                    combinedContent.append(result.get("content"));
                    if (i < results.size() - 1) {
                        combinedContent.append("\n\n");
                    }
                }
                outputs.put("combinedContent", combinedContent.toString());
            } else {
                outputs.put("combinedContent", "");
            }
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("strategy", retrieveStrategy);
            metadata.put("maxResults", maxResults);
            metadata.put("similarityThreshold", similarityThreshold);
            if (!knowledgeBaseId.isEmpty()) {
                metadata.put("knowledgeBaseId", knowledgeBaseId);
            }
            
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error performing knowledge base retrieval: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Knowledge base retrieval failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to convert Node to NodeDefDTO
     */
    private NodeDefDTO convertToNodeDefDTO(Node node) {
        NodeDefDTO nodeDefDTO = new NodeDefDTO();
        nodeDefDTO.setId(node.getId());
        nodeDefDTO.setType(node.getType());
        nodeDefDTO.setData(node.getData());
        return nodeDefDTO;
    }
    
    /**
     * Helper method to get a string parameter with default value
     */
    private String getStringParam(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
    
    /**
     * Helper method to get an integer parameter with default value
     */
    private Integer getIntParam(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Helper method to get a double parameter with default value
     */
    private Double getDoubleParam(Map<String, Object> data, String key, Double defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
} 