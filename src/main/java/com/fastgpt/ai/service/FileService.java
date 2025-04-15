package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.FileUploadDTO;
import com.fastgpt.ai.entity.ChatFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Service for handling file operations in the chat system
 */
public interface FileService {
    
    /**
     * Upload a file to the system
     * 
     * @param file The file to upload
     * @param chatId Chat ID the file belongs to (optional)
     * @param appId Application ID the file is associated with (optional)
     * @param userId User ID of the uploader
     * @param metadata Additional metadata for the file
     * @return FileUploadDTO with file information
     */
    FileUploadDTO uploadFile(
        MultipartFile file, 
        String chatId, 
        String appId,
        String userId, 
        Map<String, Object> metadata
    );
    
    /**
     * Get a file by its ID
     * 
     * @param fileId File ID
     * @return ChatFile entity
     */
    ChatFile getFileById(String fileId);
    
    /**
     * Get file content as input stream
     * 
     * @param fileId File ID
     * @return InputStream of the file content
     */
    InputStream getFileContent(String fileId);
    
    /**
     * Get all files for a chat
     * 
     * @param chatId Chat ID
     * @return List of ChatFile entities
     */
    List<ChatFile> getChatFiles(String chatId);
    
    /**
     * Delete a file
     * 
     * @param fileId File ID
     */
    void deleteFile(String fileId);
    
    /**
     * Create a token for file access
     * 
     * @param fileId File ID
     * @return Access token
     */
    String createFileToken(String fileId);
    
    /**
     * Extract text content from a file
     * 
     * @param fileId File ID
     * @return Extracted text content
     */
    String extractTextFromFile(String fileId);
    
    /**
     * Check if a file is an image
     * 
     * @param contentType File MIME type
     * @return true if the file is an image
     */
    boolean isImageFile(String contentType);
    
    /**
     * Check if a file is a document
     * 
     * @param contentType File MIME type
     * @return true if the file is a document
     */
    boolean isDocumentFile(String contentType);
} 