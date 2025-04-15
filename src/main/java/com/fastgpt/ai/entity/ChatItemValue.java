package com.fastgpt.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Entity representing a value item in a chat message
 * Can be text, image, file, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatItemValue {
    
    /**
     * Type of the value: text, file, image, audio
     */
    private String type;
    
    /**
     * Text content (if type is text)
     */
    private ChatText text;
    
    /**
     * File reference (if type is file, image, or audio)
     */
    private ChatFileRef file;
    
    /**
     * Audio reference (if type is audio)
     */
    private ChatAudio audio;
    
    /**
     * Data model for text content
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatText {
        /**
         * Text content
         */
        private String content;
    }
    
    /**
     * Data model for file reference
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatFileRef {
        /**
         * Type of file: image, file, or audio
         */
        private String type;
        
        /**
         * File name
         */
        private String name;
        
        /**
         * File URL
         */
        private String url;
    }
    
    /**
     * Data model for audio content
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatAudio {
        /**
         * Audio URL
         */
        private String url;
        
        /**
         * Duration in seconds
         */
        private Integer duration;
        
        /**
         * Text transcription if available
         */
        private String transcription;
        
        /**
         * TTS model used if this was generated
         */
        private String model;
        
        /**
         * Voice used if this was generated via TTS
         */
        private String voice;
    }
} 