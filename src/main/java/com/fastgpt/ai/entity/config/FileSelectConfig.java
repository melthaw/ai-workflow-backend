package com.fastgpt.ai.entity.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for file selection in chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileSelectConfig {
    
    /**
     * Whether file selection is open
     */
    private Boolean open;
    
    /**
     * Whether users can select document files
     */
    private Boolean canSelectFile;
    
    /**
     * Whether users can select image files
     */
    private Boolean canSelectImg;
    
    /**
     * Maximum number of files allowed
     */
    private Integer maxFiles;
    
    /**
     * Whether to use custom PDF parsing
     */
    private Boolean customPdfParse;
} 