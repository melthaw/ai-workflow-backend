package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.AudioTranscriptionResponse;
import com.fastgpt.ai.dto.ChatItemDTO;
import com.fastgpt.ai.dto.TTSResponse;
import com.fastgpt.ai.service.AudioService;
import com.fastgpt.ai.service.ChatService;
import com.fastgpt.ai.service.TTSService;
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
import java.util.List;
import java.util.Map;

/**
 * Controller for audio operations, including speech-to-text and text-to-speech
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/audio")
public class AudioController {

    private final AudioService audioService;
    private final TTSService ttsService;
    private final ChatService chatService;
    
    /**
     * Audio transcription endpoint
     */
    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribeAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "userId", defaultValue = "anonymous") String userId,
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "appId", required = false) String appId) {
        
        try {
            log.info("Transcribing audio file: {} for user: {}", file.getOriginalFilename(), userId);
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty audio file"));
            }
            
            // Check if file is an audio file
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is not an audio file"));
            }
            
            // Transcribe audio
            AudioTranscriptionResponse transcription = audioService.transcribeAudio(
                file, model, userId, chatId, appId
            );
            
            return ResponseEntity.ok(transcription);
            
        } catch (Exception e) {
            log.error("Error transcribing audio", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Generate TTS for a chat message
     */
    @PostMapping("/tts-for-message")
    public ResponseEntity<ChatItemDTO> generateTTSForMessage(
            @RequestParam("chatItemId") String chatItemId,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "voice", required = false) String voice,
            @RequestParam(value = "speed", required = false, defaultValue = "1.0") Float speed) {
        
        try {
            log.info("Generating TTS for chat item: {}, model: {}, voice: {}", 
                    chatItemId, model, voice);
            
            // Generate TTS and update chat item
            ChatItemDTO updatedItem = chatService.generateTTSForMessage(chatItemId, model, voice, speed);
            
            return ResponseEntity.ok(updatedItem);
            
        } catch (Exception e) {
            log.error("Error generating TTS for message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Stream TTS audio directly
     */
    @GetMapping(value = "/tts-stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> streamTTS(
            @RequestParam("text") String text,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "voice", required = false) String voice,
            @RequestParam(value = "speed", required = false, defaultValue = "1.0") Float speed) {
        
        try {
            log.info("Streaming TTS for text (length: {}), model: {}, voice: {}", 
                    text.length(), model, voice);
            
            // Set options
            Map<String, Object> options = Map.of("speed", speed);
            
            // Generate speech
            InputStream audioStream = ttsService.synthesizeSpeech(text, model, voice, options);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentDispositionFormData("attachment", "speech.mp3");
            
            // Return stream
            return new ResponseEntity<>(new InputStreamResource(audioStream), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error streaming TTS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get audio messages from a chat
     */
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<ChatItemDTO>> getChatAudio(@PathVariable String chatId) {
        try {
            List<ChatItemDTO> audioMessages = chatService.getChatAudioMessages(chatId);
            return ResponseEntity.ok(audioMessages);
        } catch (Exception e) {
            log.error("Error getting chat audio messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get available STT models
     */
    @GetMapping("/stt-models")
    public ResponseEntity<Map<String, Object>> getSTTModels() {
        return ResponseEntity.ok(Map.of(
            "default", audioService.getDefaultSTTModel(),
            "available", audioService.getAvailableSTTModels()
        ));
    }
    
    /**
     * Get available TTS models and voices
     */
    @GetMapping("/tts-models")
    public ResponseEntity<Map<String, Object>> getTTSModels() {
        Map<String, Object> result = Map.of(
            "models", ttsService.getAvailableTTSModels(),
            "default", ttsService.getDefaultTTSModel(),
            "voices", Map.of(
                "default", ttsService.getDefaultVoice(ttsService.getDefaultTTSModel()),
                "options", ttsService.getAvailableVoices(ttsService.getDefaultTTSModel())
            )
        );
        
        return ResponseEntity.ok(result);
    }
} 