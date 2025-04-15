package com.fastgpt.ai.entity.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for speech-to-text (Whisper) in chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhisperConfig {
    
    /**
     * Whether speech-to-text is enabled
     */
    private Boolean open;
    
    /**
     * Whether to automatically send transcribed text
     */
    private Boolean autoSend;
    
    /**
     * Whether to automatically response with Text-to-Speech
     */
    private Boolean autoTTSResponse;
    
    /**
     * Whisper model to use
     */
    private String model;
} 