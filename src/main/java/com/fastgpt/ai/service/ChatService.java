package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.ChatDTO;
import com.fastgpt.ai.dto.ChatItemDTO;
import com.fastgpt.ai.dto.request.ChatCreateRequest;
import com.fastgpt.ai.dto.request.ChatMessageRequest;
import com.fastgpt.ai.entity.ChatItemValue;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.Map;

/**
 * Service for chat operations
 */
public interface ChatService {
    
    /**
     * Create a new chat session
     * @param request Chat creation request
     * @return The created chat DTO
     */
    ChatDTO createChat(ChatCreateRequest request);
    
    /**
     * Get a chat by ID
     * @param chatId Chat ID
     * @return Chat DTO
     */
    ChatDTO getChatById(String chatId);
    
    /**
     * Get all chats by user ID
     * @param userId User ID
     * @return List of chat DTOs
     */
    List<ChatDTO> getChatsByUserId(String userId);
    
    /**
     * Get all chats by app ID
     * @param appId App ID
     * @return List of chat DTOs
     */
    List<ChatDTO> getChatsByAppId(String appId);
    
    /**
     * Get all chats by user ID and app ID
     * @param userId User ID
     * @param appId App ID
     * @return List of chat DTOs
     */
    List<ChatDTO> getChatsByUserIdAndAppId(String userId, String appId);
    
    /**
     * Delete a chat by ID
     * @param chatId Chat ID
     */
    void deleteChat(String chatId);
    
    /**
     * Send a message in a chat
     * @param request Chat message request
     * @return The AI response as a ChatItemDTO
     */
    ChatItemDTO sendMessage(ChatMessageRequest request);
    
    /**
     * Stream a message response with chunks
     * 
     * @param request The chat message request
     * @param chunkConsumer Callback receiving chunks and a flag indicating if it's the last chunk
     */
    void streamChatMessage(ChatMessageRequest request, BiConsumer<String, Boolean> chunkConsumer);
    
    /**
     * Get all messages in a chat
     * @param chatId Chat ID
     * @return List of chat item DTOs
     */
    List<ChatItemDTO> getChatMessages(String chatId);

    /**
     * Send a multi-modal message with both text and files
     * @param chatId Chat ID
     * @param appId Application ID (optional)
     * @param userId User ID
     * @param valueItems List of message content values (text and files)
     * @return AI response
     */
    ChatItemDTO sendMultiModalMessage(String chatId, String appId, String userId, List<ChatItemValue> valueItems);

    /**
     * Stream a multi-modal message with chunks
     * @param chatId Chat ID
     * @param appId Application ID (optional)
     * @param userId User ID
     * @param valueItems List of message content values (text and files)
     * @param chunkConsumer Callback for receiving text chunks
     */
    void streamMultiModalMessage(
        String chatId, 
        String appId, 
        String userId, 
        List<ChatItemValue> valueItems,
        BiConsumer<String, Boolean> chunkConsumer
    );

    /**
     * Get chat messages with pagination
     * @param chatId Chat ID
     * @param limit Maximum number of messages to return
     * @param offset Starting offset
     * @return List of chat messages
     */
    List<ChatItemDTO> getChatMessages(String chatId, int limit, int offset);

    /**
     * Get files attached to a chat
     * @param chatId Chat ID
     * @return List of file information
     */
    List<Map<String, Object>> getChatFiles(String chatId);
    
    /**
     * Generate text-to-speech for a message and store in the chat item
     * @param chatItemId Chat item ID
     * @param model TTS model to use
     * @param voice Voice to use
     * @param speed Speed/rate of speech
     * @return Updated chat item with TTS URL
     */
    ChatItemDTO generateTTSForMessage(String chatItemId, String model, String voice, Float speed);
    
    /**
     * Get chat audio messages
     * @param chatId Chat ID
     * @return List of audio items in the chat
     */
    List<ChatItemDTO> getChatAudioMessages(String chatId);
} 