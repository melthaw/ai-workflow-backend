package com.fastgpt.ai.service;

import com.fastgpt.ai.entity.ChatItem;
import com.fastgpt.ai.entity.ChatItemValue;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Service for handling multi-modal message processing
 */
public interface MultiModalService {
    
    /**
     * Convert a text message to chat item value
     * 
     * @param text The text content
     * @return ChatItemValue with text content
     */
    ChatItemValue createTextValue(String text);
    
    /**
     * Create a file value from file reference
     * 
     * @param fileType Type of file (image, file, etc.)
     * @param name File name
     * @param url File URL
     * @return ChatItemValue with file reference
     */
    ChatItemValue createFileValue(String fileType, String name, String url);
    
    /**
     * Create an audio value
     * 
     * @param url Audio file URL
     * @param duration Duration in seconds
     * @param transcription Optional transcription
     * @param model Optional TTS model if generated
     * @param voice Optional TTS voice if generated
     * @return ChatItemValue with audio content
     */
    ChatItemValue createAudioValue(String url, Integer duration, String transcription, String model, String voice);
    
    /**
     * Process uploaded files into chat item values
     * 
     * @param files List of uploaded files
     * @param chatId Chat ID
     * @param appId App ID
     * @param userId User ID
     * @return List of ChatItemValue objects
     */
    List<ChatItemValue> processUploadedFiles(
        List<MultipartFile> files,
        String chatId, 
        String appId,
        String userId
    );
    
    /**
     * Format a message with text and files for AI processing
     * 
     * @param text Text message
     * @param files Optional file attachments
     * @return Formatted message map for AI input
     */
    Map<String, Object> formatMultiModalMessage(String text, List<ChatItemValue> files);
    
    /**
     * Extract text content from a chat item
     * 
     * @param chatItem Chat item to extract from
     * @return Extracted text content
     */
    String extractTextContent(ChatItem chatItem);
    
    /**
     * Check if chat item contains images
     * 
     * @param chatItem Chat item to check
     * @return true if contains images
     */
    boolean containsImages(ChatItem chatItem);
    
    /**
     * Check if chat item contains audio
     * 
     * @param chatItem Chat item to check
     * @return true if contains audio
     */
    boolean containsAudio(ChatItem chatItem);
    
    /**
     * Extract file URLs from a chat item
     * 
     * @param chatItem Chat item to extract from
     * @return List of file URLs
     */
    List<String> extractFileUrls(ChatItem chatItem);
    
    /**
     * Extract audio URLs from a chat item
     * 
     * @param chatItem Chat item to extract from
     * @return List of audio URLs
     */
    List<String> extractAudioUrls(ChatItem chatItem);
} 