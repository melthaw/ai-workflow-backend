package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.AudioTranscriptionResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for handling audio operations
 */
public interface AudioService {
    
    /**
     * Transcribe audio to text
     * 
     * @param audioFile Audio file to transcribe
     * @param model Model name to use for transcription
     * @param userId User ID of the requestor
     * @param chatId Chat ID (optional)
     * @param appId Application ID (optional)
     * @return Transcription response with text and metadata
     */
    AudioTranscriptionResponse transcribeAudio(
        MultipartFile audioFile,
        String model,
        String userId,
        String chatId,
        String appId
    );
    
    /**
     * Get the list of available speech-to-text models
     * 
     * @return Array of available model names
     */
    String[] getAvailableSTTModels();
    
    /**
     * Get the default speech-to-text model
     * 
     * @return Default model name
     */
    String getDefaultSTTModel();
} 