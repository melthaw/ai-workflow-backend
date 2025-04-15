package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.TTSResponse;
import com.fastgpt.ai.entity.ChatFile;
import com.fastgpt.ai.service.FileService;
import com.fastgpt.ai.service.TTSService;
import com.fastgpt.ai.util.MultipartFileFromInputStream;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Text-to-Speech operations
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tts")
public class TTSController {

    private final TTSService ttsService;
    private final FileService fileService;
    
    /**
     * Synthesize speech from text and return audio
     */
    @PostMapping(value = "/synthesize", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> synthesizeSpeech(
            @RequestParam("text") String text,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "voice", required = false) String voice,
            @RequestParam(value = "speed", required = false, defaultValue = "1.0") Float speed) {
        
        log.info("Synthesizing speech for text (length: {}), model: {}, voice: {}", 
                text.length(), model, voice);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Set options
            Map<String, Object> options = new HashMap<>();
            options.put("speed", speed);
            
            // Generate speech
            InputStream audioStream = ttsService.synthesizeSpeech(text, model, voice, options);
            
            // Set headers for audio response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentDispositionFormData("attachment", "speech.mp3");
            
            // Return audio stream
            return new ResponseEntity<>(new InputStreamResource(audioStream), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error synthesizing speech", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Synthesize speech and save as file
     */
    @PostMapping("/save")
    public ResponseEntity<TTSResponse> synthesizeAndSave(
            @RequestParam("text") String text,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "voice", required = false) String voice,
            @RequestParam(value = "speed", required = false, defaultValue = "1.0") Float speed,
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "userId", defaultValue = "anonymous") String userId) {
        
        log.info("Synthesizing and saving speech for text (length: {}), model: {}, voice: {}", 
                text.length(), model, voice);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Set options
            Map<String, Object> options = new HashMap<>();
            options.put("speed", speed);
            
            // Generate speech
            InputStream audioStream = ttsService.synthesizeSpeech(text, model, voice, options);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "tts");
            metadata.put("text", text.length() > 100 ? text.substring(0, 100) + "..." : text);
            metadata.put("model", model);
            metadata.put("voice", voice);
            metadata.put("chatId", chatId);
            
            // Save to file service
            String filename = "tts-" + UUID.randomUUID().toString() + ".mp3";
            
            // Create a mock MultipartFile from the InputStream
            MultipartFile mockFile = new MultipartFileFromInputStream(audioStream, filename, "audio/mpeg");
            
            // Upload to file service
            var uploadedFile = fileService.uploadFile(mockFile, chatId, appId, userId, metadata);
            
            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Create response
            TTSResponse response = TTSResponse.builder()
                    .audioUrl(uploadedFile.getPreviewUrl())
                    .model(model)
                    .voice(voice)
                    .duration(estimateAudioDuration(text))
                    .processingTime(processingTime)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error synthesizing and saving speech", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get available TTS models and voices
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getModels() {
        Map<String, Object> result = new HashMap<>();
        
        String defaultModel = ttsService.getDefaultTTSModel();
        result.put("default", defaultModel);
        result.put("models", ttsService.getAvailableTTSModels());
        
        Map<String, Object> voices = new HashMap<>();
        for (String model : ttsService.getAvailableTTSModels()) {
            Map<String, Object> modelVoices = new HashMap<>();
            modelVoices.put("default", ttsService.getDefaultVoice(model));
            modelVoices.put("options", ttsService.getAvailableVoices(model));
            voices.put(model, modelVoices);
        }
        result.put("voices", voices);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Estimate audio duration based on text length (very rough estimation)
     */
    private int estimateAudioDuration(String text) {
        // On average, people speak at around 150 words per minute
        // Assuming average word length of 5 characters plus a space
        int characterCount = text.length();
        int estimatedWords = characterCount / 6;
        int estimatedSeconds = Math.max(1, (int)(estimatedWords / 2.5)); // 150 wpm = 2.5 wps
        return estimatedSeconds;
    }
} 