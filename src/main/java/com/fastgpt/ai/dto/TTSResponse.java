package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for text-to-speech conversion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTSResponse {
    
    /**
     * URL to the audio file
     */
    private String audioUrl;
    
    /**
     * Model used for synthesis
     */
    private String model;
    
    /**
     * Voice used for synthesis
     */
    private String voice;
    
    /**
     * Length of audio in seconds
     */
    private Integer duration;
    
    /**
     * Processing time in milliseconds
     */
    private Long processingTime;
} 