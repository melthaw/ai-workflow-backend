package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.service.TTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of TTSService using OpenAI API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TTSServiceImpl implements TTSService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;
    
    private static final String DEFAULT_TTS_MODEL = "tts-1";
    private static final String DEFAULT_TTS_HD_MODEL = "tts-1-hd";
    private static final String[] AVAILABLE_TTS_MODELS = {
        "tts-1", "tts-1-hd"
    };
    
    private static final Map<String, String[]> AVAILABLE_VOICES = Map.of(
        "tts-1", new String[]{"alloy", "echo", "fable", "onyx", "nova", "shimmer"},
        "tts-1-hd", new String[]{"alloy", "echo", "fable", "onyx", "nova", "shimmer"}
    );
    
    private static final Map<String, String> DEFAULT_VOICES = Map.of(
        "tts-1", "nova",
        "tts-1-hd", "nova"
    );

    @Override
    public InputStream synthesizeSpeech(String text, String model, String voice, Map<String, Object> options) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }
        
        // Set defaults if not provided
        if (model == null || model.isEmpty()) {
            model = DEFAULT_TTS_MODEL;
        }
        
        if (voice == null || voice.isEmpty()) {
            voice = getDefaultVoice(model);
        }
        
        // Default options
        float speed = 1.0f;
        
        // Extract options if provided
        if (options != null) {
            if (options.containsKey("speed")) {
                try {
                    speed = Float.parseFloat(options.get("speed").toString());
                    // Clamp speed between 0.25 and 4.0
                    speed = Math.min(4.0f, Math.max(0.25f, speed));
                } catch (NumberFormatException e) {
                    log.warn("Invalid speed value: {}", options.get("speed"));
                }
            }
        }
        
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", text);
            requestBody.put("voice", voice);
            requestBody.put("speed", speed);
            requestBody.put("response_format", "mp3");
            
            // Create HTTP entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // Make API call with byte array response
            String endpoint = openaiBaseUrl + "/v1/audio/speech";
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                requestEntity,
                byte[].class
            );
            
            // Check if successful
            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                return new ByteArrayInputStream(responseEntity.getBody());
            } else {
                log.error("TTS API call failed with status: {}", responseEntity.getStatusCode());
                throw new RuntimeException("Failed to synthesize speech: " + responseEntity.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error in TTS synthesis", e);
            throw new RuntimeException("Error synthesizing speech: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] getAvailableTTSModels() {
        return AVAILABLE_TTS_MODELS;
    }

    @Override
    public String[] getAvailableVoices(String model) {
        return AVAILABLE_VOICES.getOrDefault(model, AVAILABLE_VOICES.get(DEFAULT_TTS_MODEL));
    }

    @Override
    public String getDefaultTTSModel() {
        return DEFAULT_TTS_MODEL;
    }

    @Override
    public String getDefaultVoice(String model) {
        return DEFAULT_VOICES.getOrDefault(model, DEFAULT_VOICES.get(DEFAULT_TTS_MODEL));
    }
} 