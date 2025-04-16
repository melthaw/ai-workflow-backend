package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Node dispatcher for text classification
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextClassificationNodeDispatcher implements NodeDispatcher {

    private final ChatClient chatClient;
    
    private static final String NODE_TYPE = "ai.text.classification";
    
    // Default parameters
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final String DEFAULT_SYSTEM_PROMPT = 
            "You are a text classification system. Your task is to classify the input text into one of the " +
            "provided categories. Return a JSON object with the following fields:\n" +
            "- category: The best matching category\n" +
            "- confidence: A confidence score between 0.0 and 1.0\n" +
            "- explanation: A brief explanation of why this category was chosen\n\n" +
            "Only return the JSON object, no other text.";
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return classifyText(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in text classification node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Text classification failed: " + e.getMessage());
        }
    }
    
    /**
     * Process text classification based on inputs
     */
    private NodeOutDTO classifyText(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing text classification node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get configuration parameters
            String model = getStringParam(nodeData, "model", DEFAULT_MODEL);
            String customPrompt = getStringParam(nodeData, "customPrompt", "");
            
            // Get categories from config or input
            List<String> categories;
            if (inputs.containsKey("categories") && inputs.get("categories") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> inputCategories = (List<String>) inputs.get("categories");
                categories = inputCategories;
            } else if (nodeData.containsKey("categories") && nodeData.get("categories") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> configCategories = (List<String>) nodeData.get("categories");
                categories = configCategories;
            } else if (nodeData.containsKey("categories") && nodeData.get("categories") instanceof String) {
                // Handle comma-separated categories
                String categoriesStr = (String) nodeData.get("categories");
                categories = Arrays.stream(categoriesStr.split(","))
                        .map(String::trim)
                        .filter(c -> !c.isEmpty())
                        .collect(Collectors.toList());
            } else {
                return NodeOutDTO.error("No categories provided for classification");
            }
            
            if (categories.isEmpty()) {
                return NodeOutDTO.error("Empty categories list provided");
            }
            
            // Get input text
            String text = getStringParam(inputs, "text", "");
            if (text.isEmpty()) {
                return NodeOutDTO.error("No text provided for classification");
            }
            
            // Create system message
            String systemPromptText = customPrompt.isEmpty() ? DEFAULT_SYSTEM_PROMPT : customPrompt;
            systemPromptText += "\n\nAvailable categories:\n";
            for (String category : categories) {
                systemPromptText += "- " + category + "\n";
            }
            SystemMessage systemMessage = new SystemMessage(systemPromptText);
            
            // Create user message with the text to classify
            UserMessage userMessage = new UserMessage(text);
            
            // Create prompt with both messages
            List<Message> messages = new ArrayList<>();
            messages.add(systemMessage);
            messages.add(userMessage);
            
            // Set model options
            Map<String, Object> options = new HashMap<>();
            options.put("model", model);
            options.put("temperature", 0.2); // Low temperature for deterministic results
            
            // Call AI service for classification
            Prompt prompt = new Prompt(messages, options);
            log.debug("Sending classification prompt: {}", prompt);
            
            ChatResponse response = chatClient.call(prompt);
            String result = response.getResult().getOutput().getContent();
            log.debug("Received classification response: {}", result);
            
            // Parse the JSON response
            Map<String, Object> classificationResult;
            try {
                classificationResult = parseJsonResponse(result);
            } catch (Exception e) {
                log.error("Error parsing classification result: {}", e.getMessage());
                return NodeOutDTO.error("Failed to parse classification result: " + e.getMessage());
            }
            
            // Validate category is in the list
            String category = (String) classificationResult.get("category");
            if (category != null && !categories.contains(category)) {
                log.warn("Classification returned category '{}' which is not in the provided categories list", category);
                // Still proceed, as the AI might have reformatted the category name
            }
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("category", classificationResult.get("category"));
            outputs.put("confidence", classificationResult.get("confidence"));
            outputs.put("explanation", classificationResult.get("explanation"));
            outputs.put("categories", categories);
            outputs.put("text", text);
            outputs.put("rawResponse", result);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("model", model);
            metadata.put("categoryCount", categories.size());
            metadata.put("textLength", text.length());
            
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error performing text classification: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Text classification failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Simple JSON parser for demonstration purposes
     * In a real application, use a proper JSON library like Jackson
     */
    private Map<String, Object> parseJsonResponse(String jsonString) {
        // This is a simplified parser for demonstration
        // In production, use a proper JSON library
        Map<String, Object> result = new HashMap<>();
        
        // Remove any leading/trailing non-JSON content
        jsonString = jsonString.trim();
        if (jsonString.startsWith("```json")) {
            jsonString = jsonString.substring("```json".length());
        }
        if (jsonString.startsWith("```")) {
            jsonString = jsonString.substring("```".length());
        }
        if (jsonString.endsWith("```")) {
            jsonString = jsonString.substring(0, jsonString.length() - "```".length());
        }
        
        // Extract category (very simple approach)
        try {
            int categoryStart = jsonString.indexOf("\"category\"");
            if (categoryStart == -1) {
                categoryStart = jsonString.indexOf("'category'");
            }
            if (categoryStart != -1) {
                int valueStart = jsonString.indexOf(":", categoryStart) + 1;
                int valueEnd = -1;
                
                // Find the closing quote
                char quoteChar = '"';
                int quoteStart = jsonString.indexOf('"', valueStart);
                int singleQuoteStart = jsonString.indexOf('\'', valueStart);
                
                if (singleQuoteStart != -1 && (quoteStart == -1 || singleQuoteStart < quoteStart)) {
                    quoteChar = '\'';
                    valueStart = singleQuoteStart + 1;
                } else if (quoteStart != -1) {
                    valueStart = quoteStart + 1;
                }
                
                valueEnd = jsonString.indexOf(quoteChar, valueStart);
                
                if (valueEnd > valueStart) {
                    String category = jsonString.substring(valueStart, valueEnd).trim();
                    result.put("category", category);
                } else {
                    result.put("category", "Unknown");
                }
            } else {
                result.put("category", "Unknown");
            }
        } catch (Exception e) {
            result.put("category", "Error");
        }
        
        // Extract confidence (very simple approach)
        try {
            int confidenceStart = jsonString.indexOf("\"confidence\"");
            if (confidenceStart == -1) {
                confidenceStart = jsonString.indexOf("'confidence'");
            }
            if (confidenceStart != -1) {
                int valueStart = jsonString.indexOf(":", confidenceStart) + 1;
                int valueEnd = jsonString.indexOf(",", valueStart);
                if (valueEnd == -1) {
                    valueEnd = jsonString.indexOf("}", valueStart);
                }
                String confidenceStr = jsonString.substring(valueStart, valueEnd).trim();
                double confidence = Double.parseDouble(confidenceStr);
                result.put("confidence", confidence);
            } else {
                result.put("confidence", 0.0);
            }
        } catch (Exception e) {
            result.put("confidence", 0.0);
        }
        
        // Extract explanation (very simple approach)
        try {
            int explanationStart = jsonString.indexOf("\"explanation\"");
            if (explanationStart == -1) {
                explanationStart = jsonString.indexOf("'explanation'");
            }
            if (explanationStart != -1) {
                int valueStart = jsonString.indexOf(":", explanationStart) + 1;
                int valueEnd = -1;
                
                // Find the closing quote
                char quoteChar = '"';
                int quoteStart = jsonString.indexOf('"', valueStart);
                int singleQuoteStart = jsonString.indexOf('\'', valueStart);
                
                if (singleQuoteStart != -1 && (quoteStart == -1 || singleQuoteStart < quoteStart)) {
                    quoteChar = '\'';
                    valueStart = singleQuoteStart + 1;
                } else if (quoteStart != -1) {
                    valueStart = quoteStart + 1;
                }
                
                valueEnd = jsonString.indexOf(quoteChar, valueStart);
                
                if (valueEnd > valueStart) {
                    String explanation = jsonString.substring(valueStart, valueEnd).trim();
                    result.put("explanation", explanation);
                } else {
                    result.put("explanation", "No explanation available");
                }
            } else {
                result.put("explanation", "No explanation available");
            }
        } catch (Exception e) {
            result.put("explanation", "Error extracting explanation");
        }
        
        return result;
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
} 