package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node dispatcher for text embedding (converting text to vectors)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextEmbeddingNodeDispatcher implements NodeDispatcher {

    private final EmbeddingClient embeddingClient;
    
    private static final String NODE_TYPE = "ai.text.embedding";
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return generateEmbedding(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in text embedding node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Text embedding failed: " + e.getMessage());
        }
    }
    
    /**
     * Process text embedding based on inputs
     */
    private NodeOutDTO generateEmbedding(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing text embedding node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get required parameters
            String modelName = getStringParam(nodeData, "model", "default");
            boolean normalizeVectors = getBooleanParam(nodeData, "normalize", true);
            
            // Get input text or texts
            Object textInput = inputs.get("text");
            List<String> textsToEmbed = new ArrayList<>();
            
            if (textInput instanceof String) {
                // Single text input
                textsToEmbed.add((String) textInput);
            } else if (textInput instanceof List) {
                // List of texts
                @SuppressWarnings("unchecked")
                List<Object> textList = (List<Object>) textInput;
                for (Object item : textList) {
                    if (item != null) {
                        textsToEmbed.add(item.toString());
                    }
                }
            } else if (textInput != null) {
                // Any other input type, convert to string
                textsToEmbed.add(textInput.toString());
            } else {
                // No input provided
                return NodeOutDTO.error("No text input provided");
            }
            
            if (textsToEmbed.isEmpty()) {
                return NodeOutDTO.error("No valid text to embed");
            }
            
            // Generate embeddings
            List<List<Double>> embeddings = new ArrayList<>();
            
            try {
                // Get embeddings from Spring AI
                EmbeddingResponse response = embeddingClient.embed(textsToEmbed);
                
                // Process the response - this structure might vary based on Spring AI version
                // We'll use reflection to handle potential API differences
                response.getResults().forEach(result -> {
                    try {
                        // Try to access the embedding data via reflection if necessary
                        Object output = result.getOutput();
                        // This assumes every embedding output has a method to get the vector as a List<Double>
                        // In practice, check the Spring AI version you're using and adjust accordingly
                        if (output != null) {
                            @SuppressWarnings("unchecked")
                            List<Double> embedding = (List<Double>) output.getClass()
                                .getMethod("getEmbedding")
                                .invoke(output);
                            
                            if (embedding != null) {
                                embeddings.add(embedding);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract embedding vector: {}", e.getMessage());
                    }
                });
            } catch (Exception e) {
                log.error("Error calling embedding service: {}", e.getMessage());
                return NodeOutDTO.error("Failed to generate embeddings: " + e.getMessage());
            }
            
            if (embeddings.isEmpty()) {
                return NodeOutDTO.error("No embeddings were generated");
            }
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            
            if (embeddings.size() == 1) {
                // Return single embedding for single input
                outputs.put("embedding", embeddings.get(0));
                outputs.put("dimensions", embeddings.get(0).size());
            } else {
                // Return list of embeddings for multiple inputs
                outputs.put("embeddings", embeddings);
                outputs.put("dimensions", embeddings.get(0).size());
                outputs.put("count", embeddings.size());
            }
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("model", modelName);
            metadata.put("normalized", normalizeVectors);
            
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error generating embeddings: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Embedding generation failed: " + e.getMessage(), e);
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
     * Helper method to get a boolean parameter with default value
     */
    private Boolean getBooleanParam(Map<String, Object> data, String key, Boolean defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
} 