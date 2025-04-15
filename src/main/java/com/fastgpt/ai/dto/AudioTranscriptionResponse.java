package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for audio transcription
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioTranscriptionResponse {
    
    /**
     * Transcribed text from the audio
     */
    private String text;
    
    /**
     * Language detected in the audio (if available)
     */
    private String language;
    
    /**
     * Duration of the processed audio in seconds
     */
    private Integer duration;
    
    /**
     * Processing time in milliseconds
     */
    private Long processingTime;
} 