package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.common.Constants;
import com.fastgpt.ai.dto.ChatCompletionResponse;
import com.fastgpt.ai.dto.function.FunctionCallResponse;
import com.fastgpt.ai.dto.function.FunctionDefinition;
import com.fastgpt.ai.dto.openai.ChatCompletionRequest;
import com.fastgpt.ai.dto.openai.ChatMessage;
import com.fastgpt.ai.dto.openai.Choice;
import com.fastgpt.ai.dto.openai.OpenAIResponse;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.ChatConfigService;
import com.fastgpt.ai.service.OpenAIClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Implementation of AI service using OpenAI API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate;
    private final ChatConfigService chatConfigService;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;
    
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

    @Override
    public CompletableFuture<String> generateResponse(String prompt) {
        return generateResponse(prompt, defaultModel, 0.7f, 1000);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, String model) {
        return generateResponse(prompt, model, 0.7f, 1000);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, String model, float temperature, int maxTokens) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(model);
        request.setTemperature(temperature);
        request.setMaxTokens(maxTokens);
        request.setMessages(List.of(
                new ChatMessage("user", prompt)
        ));

        return openAIClient.createChatCompletion(request)
                .thenApply(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        return response.getChoices().get(0).getMessage().getContent();
                    }
                    return "";
                });
    }

    @Override
    public CompletableFuture<FunctionCallResponse> generateWithFunctions(String prompt, List<FunctionDefinition> functionDefinitions) {
        return generateWithFunctions(prompt, functionDefinitions, null);
    }

    @Override
    public CompletableFuture<FunctionCallResponse> generateWithFunctions(String prompt, List<FunctionDefinition> functionDefinitions, String systemPrompt) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(defaultModel);
        request.setTemperature(0.7f);
        
        List<ChatMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(new ChatMessage("system", systemPrompt));
        }
        messages.add(new ChatMessage("user", prompt));
        request.setMessages(messages);
        
        // Convert function definitions to OpenAI format
        if (functionDefinitions != null && !functionDefinitions.isEmpty()) {
            request.setFunctions(functionDefinitions.stream()
                    .map(FunctionDefinition::toOpenAIFormat)
                    .collect(Collectors.toList()));
            
            // Set function call policy
            boolean hasRequiredFunction = functionDefinitions.stream()
                    .anyMatch(FunctionDefinition::isRequired);
            
            if (hasRequiredFunction) {
                request.setFunctionCall("auto");
            } else {
                request.setFunctionCall("none");
            }
        }
        
        return openAIClient.createChatCompletion(request)
                .thenApply(this::processFunctionCallResponse);
    }
    
    private FunctionCallResponse processFunctionCallResponse(OpenAIResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return new FunctionCallResponse("No response from AI service", null, null);
        }
        
        Choice choice = response.getChoices().get(0);
        ChatMessage message = choice.getMessage();
        String content = message.getContent();
        
        // Check if there's a function call
        Map<String, Object> functionCall = message.getFunctionCall();
        
        if (functionCall != null) {
            String name = (String) functionCall.get("name");
            
            // Parse arguments
            String argsJson = (String) functionCall.get("arguments");
            Map<String, Object> args = null;
            
            try {
                args = objectMapper.readValue(argsJson, Map.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse function call arguments: {}", argsJson, e);
            }
            
            return new FunctionCallResponse(
                content,
                new FunctionCallResponse.FunctionCall(name, args, null),
                response
            );
        }
        
        // Regular text response
        return new FunctionCallResponse(content, null, response);
    }

    @Override
    public List<String> getAvailableModels() {
        return Constants.SUPPORTED_MODELS;
    }

    @Override
    public Map<String, Object> getModelInfo(String model) {
        // TODO: Implement model information retrieval
        return Map.of(
            "name", model,
            "maxContextLength", model.contains("gpt-4") ? 8192 : 4096,
            "tokenLimit", model.contains("gpt-4") ? 1024 : 512
        );
    }

    @Override
    public int calculateTokens(String text) {
        // Simple estimation: roughly 4 characters per token for English text
        if (text == null) return 0;
        return text.length() / 4;
    }
} 