package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a value item in a chat message
 * Can be text, image, file, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatItemValueDTO {
    
    /**
     * Type of the value: text, file, image
     */
    private String type;
    
    /**
     * Text content (if type is text)
     */
    private ChatText text;
    
    /**
     * File reference (if type is file or image)
     */
    private ChatFileRef file;
    
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
         * Type of file: image or file
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
} 