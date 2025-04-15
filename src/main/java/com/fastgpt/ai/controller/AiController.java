package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.request.AiCompletionRequest;
import com.fastgpt.ai.dto.response.AiCompletionResponse;
import com.fastgpt.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for direct AI model access
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Simple AI completion endpoint
     */
    @PostMapping("/completion")
    public AiCompletionResponse getCompletion(@Valid @RequestBody AiCompletionRequest request) {
        log.info("Received AI completion request");
        
        // Prepare model configuration
        Map<String, Object> modelConfig = new HashMap<>();
        if (request.getModel() != null) {
            modelConfig.put("model", request.getModel());
        }
        if (request.getTemperature() != null) {
            modelConfig.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            modelConfig.put("max_tokens", request.getMaxTokens());
        }
        
        // Prepare messages
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getPrompt() != null) {
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getPrompt());
            messages.add(userMessage);
        } else if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            messages.addAll(request.getMessages());
        } else {
            throw new IllegalArgumentException("Either prompt or messages must be provided");
        }
        
        // Get response
        String aiResponse = aiService.generateResponse(messages, request.getSystemPrompt(), modelConfig);
        
        return AiCompletionResponse.builder()
                .response(aiResponse)
                .status("success")
                .build();
    }
    
    /**
     * Streaming AI completion endpoint
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCompletion(@Valid @RequestBody AiCompletionRequest request) {
        log.info("Received streaming AI completion request");
        
        // Create emitter
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout
        
        // A unique ID for this completion
        String completionId = UUID.randomUUID().toString();
        
        // Prepare model configuration
        Map<String, Object> modelConfig = new HashMap<>();
        if (request.getModel() != null) {
            modelConfig.put("model", request.getModel());
        }
        if (request.getTemperature() != null) {
            modelConfig.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            modelConfig.put("max_tokens", request.getMaxTokens());
        }
        
        // Prepare messages
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getPrompt() != null) {
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getPrompt());
            messages.add(userMessage);
        } else if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            messages.addAll(request.getMessages());
        } else {
            throw new IllegalArgumentException("Either prompt or messages must be provided");
        }
        
        // Set up handlers
        emitter.onCompletion(() -> log.debug("Stream completed: {}", completionId));
        emitter.onTimeout(() -> {
            log.warn("Stream timeout: {}", completionId);
            try {
                emitter.send(SseEmitter.event()
                        .id(completionId)
                        .name("error")
                        .data("Stream timeout"));
                emitter.complete();
            } catch (IOException e) {
                log.error("Error sending timeout message: {}", e.getMessage());
            }
        });
        emitter.onError(error -> {
            log.error("Stream error: {}", error.getMessage());
            try {
                emitter.send(SseEmitter.event()
                        .id(completionId)
                        .name("error")
                        .data("Error: " + error.getMessage()));
                emitter.complete();
            } catch (IOException e) {
                log.error("Error sending error message: {}", e.getMessage());
            }
        });
        
        // Process in separate thread
        executorService.execute(() -> {
            StringBuilder responseBuilder = new StringBuilder();
            
            try {
                // Generate streaming response
                aiService.generateStreamingResponse(
                        messages,
                        request.getSystemPrompt(),
                        modelConfig,
                        (chunk, isLast) -> {
                            try {
                                // Accumulate response
                                responseBuilder.append(chunk);
                                
                                // Send chunk
                                emitter.send(SseEmitter.event()
                                        .id(completionId)
                                        .name("chunk")
                                        .data(chunk));
                                
                                // Complete on last chunk
                                if (isLast) {
                                    emitter.send(SseEmitter.event()
                                            .id(completionId)
                                            .name("done")
                                            .data(Map.of(
                                                    "status", "success",
                                                    "response", responseBuilder.toString()
                                            )));
                                    emitter.complete();
                                }
                            } catch (IOException e) {
                                log.error("Error sending chunk: {}", e.getMessage());
                                emitter.completeWithError(e);
                            }
                        });
            } catch (Exception e) {
                log.error("Error generating streaming response: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .id(completionId)
                            .name("error")
                            .data("Error: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ioe) {
                    log.error("Error sending error message: {}", ioe.getMessage());
                }
            }
        });
        
        return emitter;
    }
} 