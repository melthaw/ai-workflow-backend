package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.FileUploadDTO;
import com.fastgpt.ai.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller for file operations in chat
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    /**
     * Upload a file
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "userId", defaultValue = "anonymous") String userId,
            @RequestParam(value = "metadata", required = false) Map<String, Object> metadata) {
        
        log.info("Uploading file: {} for user: {}, chat: {}, app: {}", 
                file.getOriginalFilename(), userId, chatId, appId);
        
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        
        FileUploadDTO uploadedFile = fileService.uploadFile(file, chatId, appId, userId, metadata);
        return ResponseEntity.ok(uploadedFile);
    }

    /**
     * Get file by ID
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> getFile(
            @PathVariable String fileId,
            @RequestParam(value = "token", required = false) String token) {
        
        // Verify token (a real implementation would check token validity)
        
        try {
            // Get file metadata
            var chatFile = fileService.getFileById(fileId);
            String filename = chatFile.getOriginalName();
            
            // Get file content
            InputStream inputStream = fileService.getFileContent(fileId);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(chatFile.getContentType()));
            headers.setContentDispositionFormData("attachment", 
                    URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()));
            
            // Return file
            return new ResponseEntity<>(new InputStreamResource(inputStream), headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error getting file", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Delete a file
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable String fileId,
            @RequestParam(value = "userId") String userId) {
        
        try {
            // Get file (to check ownership)
            var chatFile = fileService.getFileById(fileId);
            
            // In a real implementation, you would check that the user has permission to delete this file
            
            // Delete file
            fileService.deleteFile(fileId);
            
            // Return success
            return ResponseEntity.ok(Map.of("status", "success", "message", "File deleted"));
        } catch (Exception e) {
            log.error("Error deleting file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * Extract text from file (for RAG)
     */
    @GetMapping("/{fileId}/extract")
    public ResponseEntity<Map<String, String>> extractText(@PathVariable String fileId) {
        try {
            String extractedText = fileService.extractTextFromFile(fileId);
            return ResponseEntity.ok(Map.of("text", extractedText));
        } catch (Exception e) {
            log.error("Error extracting text from file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
} 