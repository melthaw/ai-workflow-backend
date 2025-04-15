package com.fastgpt.ai.service;

import java.io.InputStream;
import java.util.Map;

/**
 * Service for text-to-speech operations
 */
public interface TTSService {
    
    /**
     * Convert text to speech
     * 
     * @param text Text to convert to speech
     * @param model TTS model name
     * @param voice Voice to use
     * @param options Additional options (speed, pitch)
     * @return InputStream containing audio data
     */
    InputStream synthesizeSpeech(
        String text,
        String model,
        String voice,
        Map<String, Object> options
    );
    
    /**
     * Get available TTS models
     * 
     * @return Array of available TTS model names
     */
    String[] getAvailableTTSModels();
    
    /**
     * Get available TTS voices for a model
     * 
     * @param model The model name
     * @return Array of available voice names
     */
    String[] getAvailableVoices(String model);
    
    /**
     * Get the default TTS model
     * 
     * @return Default model name
     */
    String getDefaultTTSModel();
    
    /**
     * Get the default voice for a model
     * 
     * @param model The model name
     * @return Default voice name
     */
    String getDefaultVoice(String model);
} 