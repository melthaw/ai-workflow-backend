package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.AudioTranscriptionResponse;
import com.fastgpt.ai.service.AudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the AudioService for transcription
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;
    
    private static final String DEFAULT_STT_MODEL = "whisper-1";
    private static final String[] AVAILABLE_STT_MODELS = {
        "whisper-1"
    };

    @Override
    public AudioTranscriptionResponse transcribeAudio(
            MultipartFile audioFile,
            String model,
            String userId,
            String chatId,
            String appId) {
        
        long startTime = System.currentTimeMillis();
        
        // Validate model
        if (model == null || model.isEmpty()) {
            model = DEFAULT_STT_MODEL;
        }
        
        try {
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openaiApiKey);
            
            // Create request body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Add file
            ByteArrayResource fileResource = new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();
                }
            };
            body.add("file", fileResource);
            
            // Add model
            body.add("model", model);
            
            // Optional parameters
            body.add("language", "en"); // Can be made configurable
            body.add("response_format", "json");
            
            // Create request entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Send request to OpenAI API
            String endpoint = openaiBaseUrl + "/v1/audio/transcriptions";
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                endpoint,
                requestEntity,
                Map.class
            );
            
            // Process response
            Map<String, Object> responseMap = responseEntity.getBody();
            if (responseMap == null || !responseMap.containsKey("text")) {
                throw new RuntimeException("Transcription failed: Empty or invalid response");
            }
            
            String transcribedText = (String) responseMap.get("text");
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Create response DTO
            return AudioTranscriptionResponse.builder()
                    .text(transcribedText)
                    .language((String) responseMap.getOrDefault("language", "en"))
                    .duration(calculateAudioDuration(audioFile))
                    .processingTime(processingTime)
                    .build();
            
        } catch (IOException e) {
            log.error("Error reading audio file", e);
            throw new RuntimeException("Failed to read audio file", e);
        } catch (Exception e) {
            log.error("Error transcribing audio", e);
            throw new RuntimeException("Failed to transcribe audio: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] getAvailableSTTModels() {
        return AVAILABLE_STT_MODELS;
    }

    @Override
    public String getDefaultSTTModel() {
        return DEFAULT_STT_MODEL;
    }
    
    /**
     * Estimate audio duration based on file size
     * This is a simple estimation and could be replaced with actual audio duration extraction
     */
    private int calculateAudioDuration(MultipartFile audioFile) {
        // Rough estimate: ~16KB per second for 16kHz 16-bit mono audio
        long fileSizeBytes = audioFile.getSize();
        int estimatedDurationSeconds = Math.max(1, (int) (fileSizeBytes / 16000));
        return estimatedDurationSeconds;
    }
} 