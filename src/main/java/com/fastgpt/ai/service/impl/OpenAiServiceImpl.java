package com.fastgpt.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.ChatConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiServiceImpl implements AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ChatConfigService chatConfigService;
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Override
    public String generateResponse(List<Map<String, String>> messages, String systemPrompt, Map<String, Object> modelConfig) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // Set model from config or default to gpt-3.5-turbo
            String model = modelConfig.containsKey("model") ? 
                    modelConfig.get("model").toString() : "gpt-3.5-turbo";
            requestBody.put("model", model);
            
            // Set temperature from config or default to 0.7
            double temperature = 0.7;
            if (modelConfig.containsKey("temperature")) {
                try {
                    temperature = Double.parseDouble(modelConfig.get("temperature").toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid temperature value: {}", modelConfig.get("temperature"));
                }
            }
            requestBody.put("temperature", temperature);
            
            // Set max tokens if provided
            if (modelConfig.containsKey("max_tokens")) {
                try {
                    int maxTokens = Integer.parseInt(modelConfig.get("max_tokens").toString());
                    requestBody.put("max_tokens", maxTokens);
                } catch (NumberFormatException e) {
                    log.warn("Invalid max_tokens value: {}", modelConfig.get("max_tokens"));
                }
            }
            
            // Create messages array
            ArrayNode messagesNode = requestBody.putArray("messages");
            
            // Add system prompt if provided
            if (StringUtils.hasText(systemPrompt)) {
                ObjectNode systemMessage = messagesNode.addObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
            }
            
            // Add all other messages
            for (Map<String, String> message : messages) {
                ObjectNode messageNode = messagesNode.addObject();
                messageNode.put("role", message.get("role"));
                messageNode.put("content", message.get("content"));
            }
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/v1/chat/completions", request, String.class);
            
            return extractContentFromResponse(response.getBody());
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return "I'm sorry, I encountered an error while processing your request.";
        }
    }

    @Override
    public String generateSimpleResponse(String prompt, String appId) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        // Get configuration for app if appId is provided
        String systemPrompt = "You are a helpful AI assistant.";
        Map<String, Object> modelConfig = new HashMap<>();
        modelConfig.put("model", "gpt-3.5-turbo");
        modelConfig.put("temperature", 0.7);
        
        if (StringUtils.hasText(appId)) {
            systemPrompt = chatConfigService.getSystemPrompt(appId);
            modelConfig = chatConfigService.getModelConfig(appId);
        }
        
        return generateResponse(messages, systemPrompt, modelConfig);
    }
    
    /**
     * Extract the content from OpenAI API response
     */
    private String extractContentFromResponse(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choicesNode = rootNode.path("choices");
            
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode firstChoice = choicesNode.get(0);
                JsonNode messageNode = firstChoice.path("message");
                if (!messageNode.isMissingNode()) {
                    return messageNode.path("content").asText();
                }
            }
            
            log.error("Unexpected response format: {}", responseBody);
            return "I couldn't process the response. Please try again later.";
        } catch (JsonProcessingException e) {
            log.error("Error parsing OpenAI response", e);
            return "I couldn't understand the response. Please try again later.";
        }
    }
} 