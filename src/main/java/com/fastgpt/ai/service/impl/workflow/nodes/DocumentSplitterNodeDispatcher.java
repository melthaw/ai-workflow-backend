package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Node dispatcher for splitting documents into chunks
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSplitterNodeDispatcher implements NodeDispatcher {

    private static final String NODE_TYPE = "ai.document.splitter";
    
    // Default chunk sizes
    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_CHUNK_OVERLAP = 200;
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return splitDocument(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in document splitter node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Document splitting failed: " + e.getMessage());
        }
    }
    
    /**
     * Process document splitting based on inputs
     */
    private NodeOutDTO splitDocument(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing document splitter node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get configuration parameters
            int chunkSize = getIntParam(nodeData, "chunkSize", DEFAULT_CHUNK_SIZE);
            int chunkOverlap = getIntParam(nodeData, "chunkOverlap", DEFAULT_CHUNK_OVERLAP);
            String splitMethod = getStringParam(nodeData, "splitMethod", "characters");
            boolean keepMetadata = getBooleanParam(nodeData, "keepMetadata", true);
            
            // Validate parameters
            if (chunkOverlap >= chunkSize) {
                return NodeOutDTO.error("Chunk overlap must be less than chunk size");
            }
            
            // Get input document
            String document = getStringParam(inputs, "document", "");
            if (document.isEmpty()) {
                return NodeOutDTO.error("No document provided for splitting");
            }
            
            // Get metadata if available
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = inputs.get("metadata") instanceof Map 
                ? (Map<String, Object>) inputs.get("metadata")
                : new HashMap<>();
            
            // Split the document
            List<Map<String, Object>> chunks = new ArrayList<>();
            List<String> textChunks;
            
            switch (splitMethod.toLowerCase()) {
                case "sentences":
                    textChunks = splitBySentences(document, chunkSize, chunkOverlap);
                    break;
                case "paragraphs":
                    textChunks = splitByParagraphs(document, chunkSize, chunkOverlap);
                    break;
                case "characters":
                default:
                    textChunks = splitByCharacters(document, chunkSize, chunkOverlap);
                    break;
            }
            
            // Create chunk objects with text and metadata
            for (int i = 0; i < textChunks.size(); i++) {
                Map<String, Object> chunk = new HashMap<>();
                chunk.put("text", textChunks.get(i));
                chunk.put("index", i);
                
                // Add document metadata if requested
                if (keepMetadata) {
                    chunk.put("metadata", new HashMap<>(metadata));
                }
                
                // Add chunk-specific metadata
                Map<String, Object> chunkMetadata = (Map<String, Object>) chunk.getOrDefault("metadata", new HashMap<>());
                chunkMetadata.put("chunk_index", i);
                chunkMetadata.put("chunk_total", textChunks.size());
                
                chunks.add(chunk);
            }
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("chunks", chunks);
            outputs.put("chunkCount", chunks.size());
            
            // Add metadata
            Map<String, Object> resultMetadata = new HashMap<>();
            resultMetadata.put("splitMethod", splitMethod);
            resultMetadata.put("chunkSize", chunkSize);
            resultMetadata.put("chunkOverlap", chunkOverlap);
            
            return NodeOutDTO.success(outputs, resultMetadata);
            
        } catch (Exception e) {
            log.error("Error splitting document: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Document splitting failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Split text by characters with overlap
     */
    private List<String> splitByCharacters(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        
        if (textLength <= chunkSize) {
            chunks.add(text);
            return chunks;
        }
        
        int start = 0;
        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            
            // Add chunk
            chunks.add(text.substring(start, end));
            
            // Move to next chunk position, accounting for overlap
            start = end - chunkOverlap;
            
            // Break if we've reached the end
            if (start + chunkSize > textLength && end == textLength) {
                break;
            }
        }
        
        return chunks;
    }
    
    /**
     * Split text by paragraphs, respecting chunk size constraints
     */
    private List<String> splitByParagraphs(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        
        // Split text into paragraphs (defined by double line breaks)
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        // Create chunks from paragraphs
        StringBuilder currentChunk = new StringBuilder();
        List<String> paragraphsInCurrentChunk = new ArrayList<>();
        
        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }
            
            // If adding this paragraph would exceed chunk size, finalize current chunk
            if (currentChunk.length() + trimmedParagraph.length() + 2 > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                // Handle overlap by adding some paragraphs from previous chunk
                currentChunk = new StringBuilder();
                int overlapSize = 0;
                
                // Add paragraphs from previous chunk to satisfy overlap requirement
                for (int i = paragraphsInCurrentChunk.size() - 1; i >= 0; i--) {
                    String prevParagraph = paragraphsInCurrentChunk.get(i);
                    if (overlapSize + prevParagraph.length() <= chunkOverlap) {
                        currentChunk.insert(0, prevParagraph + "\n\n");
                        overlapSize += prevParagraph.length() + 2;
                    } else {
                        break;
                    }
                }
                
                paragraphsInCurrentChunk.clear();
                if (overlapSize > 0) {
                    paragraphsInCurrentChunk.add(currentChunk.toString().trim());
                }
            }
            
            // Add paragraph to current chunk
            currentChunk.append(trimmedParagraph).append("\n\n");
            paragraphsInCurrentChunk.add(trimmedParagraph);
        }
        
        // Add the final chunk if not empty
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * Split text by sentences, respecting chunk size constraints
     */
    private List<String> splitBySentences(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        
        // Simple sentence splitting pattern
        // Note: This is a simplified approach. For more accurate sentence splitting,
        // consider using NLP libraries.
        String[] sentences = text.split("(?<=[.!?])\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        List<String> sentencesInCurrentChunk = new ArrayList<>();
        
        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) {
                continue;
            }
            
            // If adding this sentence would exceed chunk size, finalize current chunk
            if (currentChunk.length() + trimmedSentence.length() + 1 > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                // Handle overlap by adding some sentences from previous chunk
                currentChunk = new StringBuilder();
                int overlapSize = 0;
                
                // Add sentences from previous chunk to satisfy overlap requirement
                for (int i = sentencesInCurrentChunk.size() - 1; i >= 0; i--) {
                    String prevSentence = sentencesInCurrentChunk.get(i);
                    if (overlapSize + prevSentence.length() <= chunkOverlap) {
                        currentChunk.insert(0, prevSentence + " ");
                        overlapSize += prevSentence.length() + 1;
                    } else {
                        break;
                    }
                }
                
                sentencesInCurrentChunk.clear();
                if (overlapSize > 0) {
                    sentencesInCurrentChunk.add(currentChunk.toString().trim());
                }
            }
            
            // Add sentence to current chunk
            currentChunk.append(trimmedSentence).append(" ");
            sentencesInCurrentChunk.add(trimmedSentence);
        }
        
        // Add the final chunk if not empty
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
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