package com.fastgpt.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for file upload response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadDTO {
    
    /**
     * ID of the uploaded file
     */
    private String fileId;
    
    /**
     * URL for previewing the file
     */
    private String previewUrl;
    
    /**
     * Original file name
     */
    private String originalName;
    
    /**
     * File size in bytes
     */
    private Long size;
    
    /**
     * MIME type of the file
     */
    private String contentType;
} 