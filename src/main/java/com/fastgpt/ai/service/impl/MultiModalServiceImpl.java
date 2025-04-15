package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.entity.ChatItem;
import com.fastgpt.ai.entity.ChatItemValue;
import com.fastgpt.ai.service.FileService;
import com.fastgpt.ai.service.MultiModalService;
import com.fastgpt.ai.dto.FileUploadDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of MultiModalService for handling multi-modal messages
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MultiModalServiceImpl implements MultiModalService {

    private final FileService fileService;
    
    // Types of chat item values
    private static final String TYPE_TEXT = "text";
    private static final String TYPE_FILE = "file";
    private static final String TYPE_IMAGE = "image";
    private static final String TYPE_AUDIO = "audio";
    
    @Override
    public ChatItemValue createTextValue(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        ChatItemValue.ChatText chatText = ChatItemValue.ChatText.builder()
                .content(text)
                .build();
        
        return ChatItemValue.builder()
                .type(TYPE_TEXT)
                .text(chatText)
                .build();
    }

    @Override
    public ChatItemValue createFileValue(String fileType, String name, String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        ChatItemValue.ChatFileRef fileRef = ChatItemValue.ChatFileRef.builder()
                .type(fileType)
                .name(name)
                .url(url)
                .build();
        
        return ChatItemValue.builder()
                .type(fileType.equals(TYPE_IMAGE) ? TYPE_IMAGE : TYPE_FILE)
                .file(fileRef)
                .build();
    }
    
    @Override
    public ChatItemValue createAudioValue(String url, Integer duration, String transcription, String model, String voice) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Create audio metadata
        ChatItemValue.ChatAudio audio = ChatItemValue.ChatAudio.builder()
                .url(url)
                .duration(duration)
                .transcription(transcription)
                .model(model)
                .voice(voice)
                .build();
        
        // Create file reference too for compatibility
        ChatItemValue.ChatFileRef fileRef = ChatItemValue.ChatFileRef.builder()
                .type(TYPE_AUDIO)
                .name("Audio " + (transcription != null ? "(" + transcription + ")" : ""))
                .url(url)
                .build();
        
        return ChatItemValue.builder()
                .type(TYPE_AUDIO)
                .audio(audio)
                .file(fileRef)  // Include file reference for backward compatibility
                .build();
    }

    @Override
    public List<ChatItemValue> processUploadedFiles(
            List<MultipartFile> files,
            String chatId, 
            String appId,
            String userId) {
        
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<ChatItemValue> result = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // Upload file
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chatId", chatId);
                
                FileUploadDTO uploadedFile = fileService.uploadFile(
                        file, 
                        chatId, 
                        appId, 
                        userId, 
                        metadata
                );
                
                // Determine file type
                String contentType = uploadedFile.getContentType();
                String fileType;
                
                if (fileService.isImageFile(contentType)) {
                    fileType = TYPE_IMAGE;
                } else if (contentType != null && contentType.startsWith("audio/")) {
                    fileType = TYPE_AUDIO;
                    
                    // For audio files, create audio value
                    ChatItemValue audioValue = createAudioValue(
                            uploadedFile.getPreviewUrl(),
                            null,  // Duration will be determined later if needed
                            null,  // No transcription yet
                            null,  // Not TTS generated
                            null   // No voice
                    );
                    
                    if (audioValue != null) {
                        result.add(audioValue);
                        continue;
                    }
                } else {
                    fileType = TYPE_FILE;
                }
                
                // Create file value
                ChatItemValue fileValue = createFileValue(
                        fileType,
                        uploadedFile.getOriginalName(),
                        uploadedFile.getPreviewUrl()
                );
                
                result.add(fileValue);
            } catch (Exception e) {
                log.error("Error processing uploaded file: {}", e.getMessage(), e);
                // Continue with other files
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Object> formatMultiModalMessage(String text, List<ChatItemValue> files) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> content = new ArrayList<>();
        
        // Add text if present
        if (text != null && !text.isEmpty()) {
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", text);
            content.add(textContent);
        }
        
        // Add files if present
        if (files != null && !files.isEmpty()) {
            for (ChatItemValue file : files) {
                if (file.getType().equals(TYPE_IMAGE) && file.getFile() != null) {
                    Map<String, Object> imageContent = new HashMap<>();
                    imageContent.put("type", "image_url");
                    
                    Map<String, String> imageUrl = new HashMap<>();
                    imageUrl.put("url", file.getFile().getUrl());
                    imageContent.put("image_url", imageUrl);
                    
                    content.add(imageContent);
                } else if (file.getType().equals(TYPE_AUDIO) && file.getAudio() != null) {
                    // For audio, if we have a transcription, add it as text
                    if (file.getAudio().getTranscription() != null && !file.getAudio().getTranscription().isEmpty()) {
                        Map<String, Object> transcriptionContent = new HashMap<>();
                        transcriptionContent.put("type", "text");
                        transcriptionContent.put("text", "Audio transcription: " + file.getAudio().getTranscription());
                        
                        content.add(transcriptionContent);
                    }
                    
                    // Also add the audio URL as a note
                    Map<String, Object> audioUrlContent = new HashMap<>();
                    audioUrlContent.put("type", "text");
                    audioUrlContent.put("text", "Audio URL: " + file.getAudio().getUrl());
                    
                    content.add(audioUrlContent);
                } else if (file.getType().equals(TYPE_FILE) && file.getFile() != null) {
                    Map<String, Object> fileContent = new HashMap<>();
                    fileContent.put("type", "file_url");
                    fileContent.put("name", file.getFile().getName());
                    fileContent.put("url", file.getFile().getUrl());
                    
                    content.add(fileContent);
                }
            }
        }
        
        result.put("role", "user");
        result.put("content", content);
        
        return result;
    }

    @Override
    public String extractTextContent(ChatItem chatItem) {
        // If there are no value items, use legacy 'value' field
        if (chatItem.getValueItems() == null || chatItem.getValueItems().isEmpty()) {
            return chatItem.getValue();
        }
        
        // Collect text from all text-type value items and audio transcriptions
        StringBuilder textBuilder = new StringBuilder();
        
        for (ChatItemValue item : chatItem.getValueItems()) {
            // Add text content
            if (TYPE_TEXT.equals(item.getType()) && item.getText() != null && item.getText().getContent() != null) {
                textBuilder.append(item.getText().getContent()).append("\n");
            }
            
            // Add audio transcriptions if available
            if (TYPE_AUDIO.equals(item.getType()) && item.getAudio() != null && 
                    item.getAudio().getTranscription() != null && !item.getAudio().getTranscription().isEmpty()) {
                textBuilder.append("Audio transcription: ").append(item.getAudio().getTranscription()).append("\n");
            }
        }
        
        return textBuilder.toString().trim();
    }

    @Override
    public boolean containsImages(ChatItem chatItem) {
        if (chatItem.getValueItems() == null || chatItem.getValueItems().isEmpty()) {
            return false;
        }
        
        return chatItem.getValueItems().stream()
                .anyMatch(item -> TYPE_IMAGE.equals(item.getType()));
    }
    
    @Override
    public boolean containsAudio(ChatItem chatItem) {
        if (chatItem.getValueItems() == null || chatItem.getValueItems().isEmpty()) {
            return false;
        }
        
        return chatItem.getValueItems().stream()
                .anyMatch(item -> TYPE_AUDIO.equals(item.getType()));
    }

    @Override
    public List<String> extractFileUrls(ChatItem chatItem) {
        if (chatItem.getValueItems() == null || chatItem.getValueItems().isEmpty()) {
            return Collections.emptyList();
        }
        
        return chatItem.getValueItems().stream()
                .filter(item -> TYPE_FILE.equals(item.getType()) || TYPE_IMAGE.equals(item.getType()))
                .filter(item -> item.getFile() != null && item.getFile().getUrl() != null)
                .map(item -> item.getFile().getUrl())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> extractAudioUrls(ChatItem chatItem) {
        if (chatItem.getValueItems() == null || chatItem.getValueItems().isEmpty()) {
            return Collections.emptyList();
        }
        
        return chatItem.getValueItems().stream()
                .filter(item -> TYPE_AUDIO.equals(item.getType()))
                .map(item -> {
                    if (item.getAudio() != null && item.getAudio().getUrl() != null) {
                        return item.getAudio().getUrl();
                    } else if (item.getFile() != null && item.getFile().getUrl() != null) {
                        return item.getFile().getUrl();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
} 