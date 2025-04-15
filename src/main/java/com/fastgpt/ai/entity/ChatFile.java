package com.fastgpt.ai.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Chat file entity representing files uploaded during chat
 */
@Data
@Document(collection = "chat_files")
public class ChatFile {
    
    @Id
    private String id;
    
    /**
     * Unique file ID for reference
     */
    @Field("file_id")
    private String fileId;
    
    /**
     * ID of the team that owns this file
     */
    @Field("team_id")
    private String teamId;
    
    /**
     * ID of the user who uploaded this file
     */
    @Field("user_id")
    private String userId;
    
    /**
     * ID of the chat this file belongs to
     */
    @Field("chat_id")
    private String chatId;
    
    /**
     * Application ID if the file is associated with an app
     */
    @Field("app_id")
    private String appId;
    
    /**
     * Original file name
     */
    @Field("original_name")
    private String originalName;
    
    /**
     * File content type (MIME type)
     */
    @Field("content_type")
    private String contentType;
    
    /**
     * Size of the file in bytes
     */
    private Long size;
    
    /**
     * Storage path of the file
     */
    private String path;
    
    /**
     * Type of the file (image, document, etc)
     */
    private String type;
    
    /**
     * URL for file preview
     */
    @Field("preview_url")
    private String previewUrl;
    
    /**
     * File creation time
     */
    @Field("create_time")
    private LocalDateTime createTime;
    
    /**
     * Additional metadata for the file
     */
    private Map<String, Object> metadata;
} 