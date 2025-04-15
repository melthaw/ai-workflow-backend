package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for audio transcription
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioTranscriptionRequest {
    
    /**
     * Application ID
     */
    private String appId;
    
    /**
     * Chat ID
     */
    private String chatId;
    
    /**
     * User ID
     */
    private String userId;
    
    /**
     * Duration of the audio in seconds
     */
    private Integer duration;
    
    /**
     * The model to use for speech-to-text
     */
    private String model;
} 