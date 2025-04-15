package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.ChatConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Implementation of AI service using OpenAI API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate;
    private final ChatConfigService chatConfigService;
    
    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;
    
    @Value("${spring.ai.openai.model:gpt-3.5-turbo}")
    private String defaultModel;

    @Override
    public String generateResponse(List<Map<String, String>> messages, String systemPrompt, Map<String, Object> modelConfig) {
        // Build the request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelConfig != null && modelConfig.containsKey("model") ? 
                modelConfig.get("model") : defaultModel);
        
        // Add temperature if specified
        if (modelConfig != null && modelConfig.containsKey("temperature")) {
            requestBody.put("temperature", modelConfig.get("temperature"));
        }
        
        // Add max tokens if specified
        if (modelConfig != null && modelConfig.containsKey("max_tokens")) {
            requestBody.put("max_tokens", modelConfig.get("max_tokens"));
        }
        
        // Prepare messages array
        List<Map<String, String>> apiMessages = new ArrayList<>();
        
        // Add system message if provided
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            apiMessages.add(systemMessage);
        }
        
        // Add user messages
        apiMessages.addAll(messages);
        
        requestBody.put("messages", apiMessages);
        
        // Prepare HTTP request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            // Make API call
            ResponseEntity<Map> response = restTemplate.exchange(
                    openaiBaseUrl + "/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            
            // Extract response text
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null && message.containsKey("content")) {
                        return (String) message.get("content");
                    }
                }
            }
            
            return "I couldn't generate a proper response.";
        } catch (Exception e) {
            log.error("Error generating AI response: {}", e.getMessage(), e);
            return "Sorry, I encountered an error: " + e.getMessage();
        }
    }

    @Override
    public void generateStreamingResponse(List<Map<String, String>> messages, String systemPrompt, 
                                         Map<String, Object> modelConfig, BiConsumer<String, Boolean> chunkConsumer) {
        // Build the request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelConfig != null && modelConfig.containsKey("model") ? 
                modelConfig.get("model") : defaultModel);
        requestBody.put("stream", true);
        
        // Add temperature if specified
        if (modelConfig != null && modelConfig.containsKey("temperature")) {
            requestBody.put("temperature", modelConfig.get("temperature"));
        }
        
        // Add max tokens if specified
        if (modelConfig != null && modelConfig.containsKey("max_tokens")) {
            requestBody.put("max_tokens", modelConfig.get("max_tokens"));
        }
        
        // Prepare messages array
        List<Map<String, String>> apiMessages = new ArrayList<>();
        
        // Add system message if provided
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            apiMessages.add(systemMessage);
        }
        
        // Add user messages
        apiMessages.addAll(messages);
        
        requestBody.put("messages", apiMessages);
        
        // Prepare HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        
        // The final request entity
        final HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            // Create request callback
            RequestCallback requestCallback = request -> {
                request.getHeaders().addAll(entity.getHeaders());
                try {
                    org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter = 
                            new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter();
                    converter.write(entity.getBody(), MediaType.APPLICATION_JSON, request);
                } catch (Exception e) {
                    log.error("Error writing request body", e);
                }
            };
            
            // Create response extractor
            ResponseExtractor<Void> responseExtractor = response -> {
                try (Scanner scanner = new Scanner(response.getBody())) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        
                        // Skip empty lines and the "DONE" marker
                        if (line.isEmpty() || line.equals("data: [DONE]")) {
                            continue;
                        }
                        
                        // Process data lines
                        if (line.startsWith("data: ")) {
                            try {
                                String jsonPart = line.substring(6);
                                
                                // Skip empty JSON
                                if (jsonPart.isEmpty()) {
                                    continue;
                                }
                                
                                // Extract content from JSON
                                String content = extractContent(jsonPart);
                                if (content != null && !content.isEmpty()) {
                                    chunkConsumer.accept(content, false);
                                }
                            } catch (Exception e) {
                                log.debug("Error processing line: {}", line, e);
                            }
                        }
                    }
                    
                    // Signal that we're done
                    chunkConsumer.accept("", true);
                    
                } catch (Exception e) {
                    log.error("Error reading streaming response", e);
                    chunkConsumer.accept("Error: " + e.getMessage(), true);
                }
                
                return null;
            };
            
            // Execute the request
            restTemplate.execute(
                    openaiBaseUrl + "/v1/chat/completions",
                    HttpMethod.POST,
                    requestCallback,
                    responseExtractor
            );
            
        } catch (Exception e) {
            log.error("Error in streaming request", e);
            chunkConsumer.accept("Error: " + e.getMessage(), true);
        }
    }

    @Override
    public String generateSimpleResponse(String prompt, String appId) {
        // Create a simple message
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        
        List<Map<String, String>> messages = Collections.singletonList(message);
        
        // Get model config based on appId
        Map<String, Object> modelConfig = appId != null ? 
                chatConfigService.getModelConfig(appId) : Collections.emptyMap();
        
        // Generate response
        return generateResponse(messages, null, modelConfig);
    }
    
    /**
     * Extract content from an OpenAI streaming response JSON part
     */
    private String extractContent(String json) {
        try {
            // For simplicity, we'll do a basic string check rather than full JSON parsing
            // In production, you should use a proper JSON parser
            if (json.contains("\"delta\"") && json.contains("\"content\"")) {
                int contentStart = json.indexOf("\"content\"") + 11; // Length of "content":"
                
                if (contentStart > 10) { // Make sure we found it
                    // Find the closing quote
                    int contentEnd = json.indexOf("\"", contentStart);
                    
                    if (contentEnd > contentStart) {
                        // Extract and unescape the content
                        return json.substring(contentStart, contentEnd)
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting content from JSON", e);
        }
        
        return "";
    }
} 