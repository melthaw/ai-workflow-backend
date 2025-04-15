package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.ChatDTO;
import com.fastgpt.ai.dto.ChatItemDTO;
import com.fastgpt.ai.dto.request.ChatCreateRequest;
import com.fastgpt.ai.dto.request.ChatMessageRequest;
import com.fastgpt.ai.dto.response.ApiResponse;
import com.fastgpt.ai.entity.ChatItem;
import com.fastgpt.ai.entity.ChatItemValue;
import com.fastgpt.ai.service.ChatService;
import com.fastgpt.ai.service.MultiModalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for chat operations, supporting multimodal content
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MultiModalService multiModalService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatDTO>> createChat(@Valid @RequestBody ChatCreateRequest request) {
        log.info("Creating chat for user: {}, app: {}", request.getUserId(), request.getAppId());
        ChatDTO createdChat = chatService.createChat(request);
        return ResponseEntity.ok(ApiResponse.success(createdChat));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ApiResponse<ChatDTO>> getChatById(@PathVariable String chatId) {
        log.info("Getting chat with ID: {}", chatId);
        ChatDTO chat = chatService.getChatById(chatId);
        return ResponseEntity.ok(ApiResponse.success(chat));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ChatDTO>>> getChatsByUserId(@PathVariable String userId) {
        log.info("Getting all chats for user: {}", userId);
        List<ChatDTO> chats = chatService.getChatsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(chats));
    }

    @GetMapping("/app/{appId}")
    public ResponseEntity<ApiResponse<List<ChatDTO>>> getChatsByAppId(@PathVariable String appId) {
        log.info("Getting all chats for app: {}", appId);
        List<ChatDTO> chats = chatService.getChatsByAppId(appId);
        return ResponseEntity.ok(ApiResponse.success(chats));
    }

    @GetMapping("/user/{userId}/app/{appId}")
    public ResponseEntity<ApiResponse<List<ChatDTO>>> getChatsByUserIdAndAppId(
            @PathVariable String userId,
            @PathVariable String appId) {
        log.info("Getting all chats for user: {} and app: {}", userId, appId);
        List<ChatDTO> chats = chatService.getChatsByUserIdAndAppId(userId, appId);
        return ResponseEntity.ok(ApiResponse.success(chats));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<ApiResponse<Void>> deleteChat(@PathVariable String chatId) {
        log.info("Deleting chat with ID: {}", chatId);
        chatService.deleteChat(chatId);
        return ResponseEntity.ok(ApiResponse.success("Chat deleted successfully", null));
    }

    /**
     * Send a message with optional files
     */
    @PostMapping(value = "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatItemDTO> sendMessage(
            @RequestParam(value = "message", required = false) String message,
            @RequestParam("chatId") String chatId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "userId", defaultValue = "anonymous") String userId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        try {
            log.info("Sending message to chatId: {}, appId: {}, files: {}", 
                    chatId, appId, (files != null ? files.size() : 0));
            
            // Process uploaded files
            List<ChatItemValue> fileValues = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                fileValues = multiModalService.processUploadedFiles(files, chatId, appId, userId);
            }
            
            // Create text value if message exists
            if (message != null && !message.isEmpty()) {
                ChatItemValue textValue = multiModalService.createTextValue(message);
                if (textValue != null) {
                    fileValues.add(0, textValue); // Add text as first item
                }
            }
            
            // Ensure we have something to send
            if (fileValues.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // Send message
            ChatItemDTO response = chatService.sendMultiModalMessage(
                    chatId, 
                    appId, 
                    userId, 
                    fileValues);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending chat message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Stream a message with optional files
     */
    @PostMapping(value = "/messages/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @RequestParam(value = "message", required = false) String message,
            @RequestParam("chatId") String chatId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "userId", defaultValue = "anonymous") String userId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        log.info("Streaming message to chatId: {}, appId: {}, files: {}", 
                chatId, appId, (files != null ? files.size() : 0));
        
        // Create emitter with timeout
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes
        
        try {
            // Process uploaded files
            List<ChatItemValue> fileValues = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                fileValues = multiModalService.processUploadedFiles(files, chatId, appId, userId);
            }
            
            // Create text value if message exists
            if (message != null && !message.isEmpty()) {
                ChatItemValue textValue = multiModalService.createTextValue(message);
                if (textValue != null) {
                    fileValues.add(0, textValue); // Add text as first item
                }
            }
            
            // Ensure we have something to send
            if (fileValues.isEmpty()) {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("No message content provided"));
                emitter.complete();
                return emitter;
            }
            
            // Configure emitter callbacks
            emitter.onCompletion(() -> log.debug("Stream completed for chat: {}", chatId));
            emitter.onTimeout(() -> log.warn("Stream timeout for chat: {}", chatId));
            emitter.onError((e) -> log.error("Stream error for chat: {}", chatId, e));
            
            // Stream message
            chatService.streamMultiModalMessage(
                    chatId, 
                    appId, 
                    userId, 
                    fileValues,
                    (chunk, isLast) -> {
                        try {
                            // Send chunk
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(chunk));
                            
                            // Complete if last chunk
                            if (isLast) {
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(""));
                                emitter.complete();
                            }
                        } catch (Exception e) {
                            log.error("Error sending stream chunk", e);
                            emitter.completeWithError(e);
                        }
                    });
            
        } catch (Exception e) {
            log.error("Error streaming chat message", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Error: " + e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
        
        return emitter;
    }
    
    /**
     * Get chat history
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatItemDTO>> getChatMessages(
            @PathVariable String chatId,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        
        try {
            List<ChatItemDTO> messages = chatService.getChatMessages(chatId, limit, offset);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting chat messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get file uploads for a chat
     */
    @GetMapping("/{chatId}/files")
    public ResponseEntity<Map<String, Object>> getChatFiles(
            @PathVariable String chatId) {
        
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("files", chatService.getChatFiles(chatId));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting chat files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
} 