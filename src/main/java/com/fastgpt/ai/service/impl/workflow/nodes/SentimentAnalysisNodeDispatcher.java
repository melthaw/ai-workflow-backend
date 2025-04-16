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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node dispatcher for text sentiment analysis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentimentAnalysisNodeDispatcher implements NodeDispatcher {

    private final ChatClient chatClient;
    
    private static final String NODE_TYPE = "ai.text.sentiment";
    
    // Default parameters
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String SYSTEM_PROMPT_TEMPLATE = 
            "You are a sentiment analysis expert. Analyze the sentiment of the following text " +
            "and return a JSON object with the following fields:\n" +
            "- sentiment: The sentiment label (POSITIVE, NEGATIVE, or NEUTRAL)\n" +
            "- score: A sentiment score between -1.0 (very negative) and 1.0 (very positive), with 0 being neutral\n" +
            "- explanation: A brief explanation of why this sentiment was detected\n\n" +
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
            return analyzeSentiment(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in sentiment analysis node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Sentiment analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Process sentiment analysis based on inputs
     */
    private NodeOutDTO analyzeSentiment(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing sentiment analysis node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get configuration parameters
            String model = getStringParam(nodeData, "model", DEFAULT_MODEL);
            String language = getStringParam(nodeData, "language", DEFAULT_LANGUAGE);
            
            // Get input text
            String text = getStringParam(inputs, "text", "");
            if (text.isEmpty()) {
                return NodeOutDTO.error("No text provided for sentiment analysis");
            }
            
            // Create system message
            String systemPromptText = SYSTEM_PROMPT_TEMPLATE;
            if (!"en".equals(language)) {
                systemPromptText += "\nPlease analyze the text in " + language + " language.";
            }
            SystemMessage systemMessage = new SystemMessage(systemPromptText);
            
            // Create user message with the text to analyze
            UserMessage userMessage = new UserMessage(text);
            
            // Create prompt with both messages
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(systemMessage);
            messages.add(userMessage);
            
            // Set model options
            Map<String, Object> options = new HashMap<>();
            options.put("model", model);
            options.put("temperature", 0.1); // Low temperature for more deterministic responses
            
            // Call AI service for sentiment analysis
            Prompt prompt = new Prompt(messages, options);
            log.debug("Sending sentiment analysis prompt: {}", prompt);
            
            ChatResponse response = chatClient.call(prompt);
            String result = response.getResult().getOutput().getContent();
            log.debug("Received sentiment analysis response: {}", result);
            
            // Parse the JSON response
            Map<String, Object> sentimentResult;
            try {
                // In a real implementation, use a proper JSON parser
                // For simplicity, we're assuming the response is a valid JSON object
                sentimentResult = parseJsonResponse(result);
            } catch (Exception e) {
                log.error("Error parsing sentiment analysis result: {}", e.getMessage());
                return NodeOutDTO.error("Failed to parse sentiment analysis result: " + e.getMessage());
            }
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("sentiment", sentimentResult.get("sentiment"));
            outputs.put("score", sentimentResult.get("score"));
            outputs.put("explanation", sentimentResult.get("explanation"));
            outputs.put("text", text);
            outputs.put("rawResponse", result);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("model", model);
            metadata.put("language", language);
            metadata.put("textLength", text.length());
            
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error performing sentiment analysis: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Sentiment analysis failed: " + e.getMessage(), e);
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
        
        // Extract fields (this is a very simple approach)
        if (jsonString.contains("\"sentiment\"") || jsonString.contains("'sentiment'")) {
            if (jsonString.contains("POSITIVE")) {
                result.put("sentiment", "POSITIVE");
            } else if (jsonString.contains("NEGATIVE")) {
                result.put("sentiment", "NEGATIVE");
            } else {
                result.put("sentiment", "NEUTRAL");
            }
        } else {
            result.put("sentiment", "NEUTRAL");
        }
        
        // Extract score (very simple approach)
        try {
            int scoreStart = jsonString.indexOf("\"score\"");
            if (scoreStart == -1) {
                scoreStart = jsonString.indexOf("'score'");
            }
            if (scoreStart != -1) {
                int valueStart = jsonString.indexOf(":", scoreStart) + 1;
                int valueEnd = jsonString.indexOf(",", valueStart);
                if (valueEnd == -1) {
                    valueEnd = jsonString.indexOf("}", valueStart);
                }
                String scoreStr = jsonString.substring(valueStart, valueEnd).trim();
                result.put("score", Double.parseDouble(scoreStr));
            } else {
                result.put("score", 0.0);
            }
        } catch (Exception e) {
            result.put("score", 0.0);
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