package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.request.ChatCompletionRequest;
import com.fastgpt.ai.dto.request.ChatMessageRequest;
import com.fastgpt.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller handling chat completions with support for streaming responses
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatCompletionsController {

    private final ChatService chatService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Process chat completion requests with support for streaming responses
     */
    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter processCompletion(@Valid @RequestBody ChatCompletionRequest completionRequest) {
        log.info("Received chat completion request: chatId={}, stream={}", 
                completionRequest.getChatId(), completionRequest.isStream());
        
        // Create a new emitter with a longer timeout
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout
        
        // A unique ID for this completion
        String completionId = UUID.randomUUID().toString();
        
        // Create a message request
        ChatMessageRequest messageRequest = convertToMessageRequest(completionRequest);
        
        // Handle connection close
        emitter.onCompletion(() -> {
            log.debug("Completion finished: {}", completionId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("Connection timeout: {}", completionId);
            emitter.complete();
        });
        
        emitter.onError(error -> {
            log.error("Error during streaming completion {}: {}", completionId, error.getMessage(), error);
            emitter.complete();
        });
        
        // Use an executor to process the request asynchronously
        executorService.execute(() -> {
            try {
                if (completionRequest.isStream()) {
                    // Stream mode - send events as they arrive
                    chatService.streamChatMessage(messageRequest, (chunk, isLast) -> {
                        try {
                            // Send SSE event with the chunk
                            emitter.send(SseEmitter.event()
                                    .id(completionId)
                                    .name("chunk")
                                    .data(chunk));
                            
                            // Complete if this is the last chunk
                            if (isLast) {
                                emitter.send(SseEmitter.event()
                                        .id(completionId)
                                        .name("done")
                                        .data(""));
                                emitter.complete();
                            }
                        } catch (IOException e) {
                            log.error("Error sending chunk for {}: {}", completionId, e.getMessage());
                            emitter.completeWithError(e);
                        }
                    });
                } else {
                    // Non-streaming mode - send complete response at once
                    var chatItemDTO = chatService.sendMessage(messageRequest);
                    
                    // Send the full response as a single event
                    emitter.send(SseEmitter.event()
                            .id(completionId)
                            .name("message")
                            .data(chatItemDTO));
                    
                    // Complete the emitter
                    emitter.send(SseEmitter.event()
                            .id(completionId)
                            .name("done")
                            .data(""));
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("Error processing chat completion {}: {}", completionId, e.getMessage(), e);
                try {
                    // Send error event
                    emitter.send(SseEmitter.event()
                            .id(completionId)
                            .name("error")
                            .data("Error processing chat: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ioe) {
                    emitter.completeWithError(ioe);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * Convert a completion request to a message request
     */
    private ChatMessageRequest convertToMessageRequest(ChatCompletionRequest completionRequest) {
        return ChatMessageRequest.builder()
                .chatId(completionRequest.getChatId())
                .appId(completionRequest.getAppId())
                .message(completionRequest.getMessage())
                .userId(completionRequest.getUserId())
                .teamId(completionRequest.getTeamId())
                .useRag(completionRequest.isUseRag())
                .kbIds(completionRequest.getKbIds())
                .limit(completionRequest.getLimit())
                .minScore(completionRequest.getMinScore())
                .metadata(completionRequest.getMetadata())
                .build();
    }
} 