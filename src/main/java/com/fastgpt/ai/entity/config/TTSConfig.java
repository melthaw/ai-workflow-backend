package com.fastgpt.ai.entity.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for text-to-speech in chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTSConfig {
    
    /**
     * Whether text-to-speech is enabled
     */
    private Boolean open;
    
    /**
     * The TTS model to use
     */
    private String model;
    
    /**
     * The voice ID to use
     */
    private String voice;
    
    /**
     * Speech rate (speed)
     */
    private Float rate;
    
    /**
     * Speech pitch
     */
    private Float pitch;
} 