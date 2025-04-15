package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.FileUploadDTO;
import com.fastgpt.ai.entity.ChatFile;
import com.fastgpt.ai.repository.ChatFileRepository;
import com.fastgpt.ai.service.FileService;
import com.fastgpt.ai.util.FileUtils;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

/**
 * Implementation of FileService for handling file operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final ChatFileRepository chatFileRepository;
    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;
    private final Tika tika = new Tika();
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${app.file.preview-url:/api/v1/files}")
    private String previewUrlBase;
    
    @Value("${app.file.max-size:5242880}") // 5MB default
    private long maxFileSize;
    
    @Value("${app.file.token-secret:changeme}")
    private String tokenSecret;
    
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    private static final String[] DOCUMENT_EXTENSIONS = {
        "pdf", "doc", "docx", "txt", "rtf", "csv", "xls", "xlsx", "ppt", "pptx"
    };
    
    private static final String[] IMAGE_EXTENSIONS = {
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp"
    };

    @Override
    public FileUploadDTO uploadFile(
            MultipartFile file, 
            String chatId, 
            String appId,
            String userId, 
            Map<String, Object> metadata) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }
            
            if (file.getSize() > maxFileSize) {
                throw new IllegalArgumentException("File size exceeds maximum limit");
            }
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique file ID and create file path
            String fileId = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                    FileUtils.getFileExtension(originalFilename) : "";
            
            String filename = fileId + (extension.isEmpty() ? "" : "." + extension);
            Path filePath = uploadPath.resolve(filename);
            
            // Save file to disk
            File targetFile = filePath.toFile();
            try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                outputStream.write(file.getBytes());
            }
            
            // Detect content type
            String contentType = file.getContentType();
            if (contentType == null || contentType.equals("application/octet-stream")) {
                contentType = tika.detect(targetFile);
            }
            
            // Determine file type
            String fileType = determineFileType(contentType);
            
            // Store in MongoDB GridFS
            ObjectId gridFsId;
            try (InputStream inputStream = new FileInputStream(targetFile)) {
                gridFsId = gridFsTemplate.store(
                    inputStream, 
                    filename, 
                    contentType, 
                    metadata
                );
            }
            
            // Create ChatFile entity
            ChatFile chatFile = new ChatFile();
            chatFile.setFileId(fileId);
            chatFile.setUserId(userId);
            chatFile.setChatId(chatId);
            chatFile.setAppId(appId);
            chatFile.setOriginalName(originalFilename);
            chatFile.setContentType(contentType);
            chatFile.setSize(file.getSize());
            chatFile.setPath(filePath.toString());
            chatFile.setType(fileType);
            chatFile.setCreateTime(LocalDateTime.now());
            chatFile.setMetadata(metadata);
            
            // Create preview URL with token
            String token = createFileToken(fileId);
            String previewUrl = String.format("%s/%s?token=%s", previewUrlBase, fileId, token);
            chatFile.setPreviewUrl(previewUrl);
            
            // Save to database
            chatFileRepository.save(chatFile);
            
            // Delete local file after upload
            Files.deleteIfExists(filePath);
            
            // Return upload DTO
            return FileUploadDTO.builder()
                    .fileId(fileId)
                    .previewUrl(previewUrl)
                    .originalName(originalFilename)
                    .size(file.getSize())
                    .contentType(contentType)
                    .build();
            
        } catch (Exception e) {
            log.error("Error uploading file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatFile getFileById(String fileId) {
        return chatFileRepository.findByFileId(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "fileId", fileId));
    }

    @Override
    public InputStream getFileContent(String fileId) {
        try {
            ChatFile chatFile = getFileById(fileId);
            
            // Query GridFS for the file
            GridFSFile gridFSFile = gridFsTemplate.findOne(
                new Query(Criteria.where("filename").is(chatFile.getFileId()))
            );
            
            if (gridFSFile == null) {
                throw new ResourceNotFoundException("File content", "fileId", fileId);
            }
            
            return gridFsOperations.getResource(gridFSFile).getInputStream();
        } catch (Exception e) {
            log.error("Error retrieving file content", e);
            throw new RuntimeException("Failed to retrieve file content", e);
        }
    }

    @Override
    public List<ChatFile> getChatFiles(String chatId) {
        return chatFileRepository.findByChatId(chatId);
    }

    @Override
    public void deleteFile(String fileId) {
        ChatFile chatFile = getFileById(fileId);
        
        // Delete from GridFS
        gridFsTemplate.delete(new Query(Criteria.where("filename").is(chatFile.getFileId())));
        
        // Delete from repository
        chatFileRepository.deleteByFileId(fileId);
    }

    @Override
    public String createFileToken(String fileId) {
        // Create a JWT token with 24 hour expiry
        return Jwts.builder()
                .setSubject(fileId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
                .signWith(key)
                .compact();
    }

    @Override
    public String extractTextFromFile(String fileId) {
        ChatFile chatFile = getFileById(fileId);
        String contentType = chatFile.getContentType();
        
        try (InputStream inputStream = getFileContent(fileId)) {
            // For plain text files
            if (contentType != null && contentType.startsWith("text/")) {
                return new String(inputStream.readAllBytes());
            }
            
            // For PDF files
            if (contentType != null && contentType.equals("application/pdf")) {
                return FileUtils.extractTextFromPdf(inputStream);
            }
            
            // For other document types, we would need specific handlers
            // e.g., Apache POI for Office documents
            
            if (FileUtils.isDocumentFile(contentType)) {
                return "Text extraction is not fully implemented for this document type: " + contentType;
            }
            
            return "Text extraction not supported for this file type: " + contentType;
        } catch (Exception e) {
            log.error("Error extracting text from file", e);
            return "Error extracting text: " + e.getMessage();
        }
    }

    @Override
    public boolean isImageFile(String contentType) {
        return FileUtils.isImageFile(contentType);
    }

    @Override
    public boolean isDocumentFile(String contentType) {
        return FileUtils.isDocumentFile(contentType);
    }
    
    /**
     * Determine the file type based on content type
     */
    private String determineFileType(String contentType) {
        if (FileUtils.isImageFile(contentType)) {
            return "image";
        } else if (FileUtils.isDocumentFile(contentType)) {
            return "file";
        } else if (FileUtils.isAudioFile(contentType)) {
            return "audio";
        } else {
            return "unknown";
        }
    }
} 