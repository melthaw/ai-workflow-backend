package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.FileService;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.MediaMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Node dispatcher for multimodal content processing (text + images)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultimodalProcessingNodeDispatcher implements NodeDispatcher {

    private final ChatClient chatClient;
    private final Optional<FileService> fileService;
    
    private static final String NODE_TYPE = "ai.multimodal.processing";
    
    // Default parameters
    private static final String DEFAULT_MODEL = "gpt-4-vision-preview";
    private static final String DEFAULT_SYSTEM_PROMPT = 
            "You are an AI assistant capable of understanding and analyzing images along with text. " +
            "Provide detailed and accurate descriptions of the visual content and respond to any questions about it.";
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return processMultimodalContent(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in multimodal processing node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Multimodal processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Process multimodal content based on inputs
     */
    private NodeOutDTO processMultimodalContent(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing multimodal content node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get configuration parameters
            String model = getStringParam(nodeData, "model", DEFAULT_MODEL);
            String systemPrompt = getStringParam(nodeData, "systemPrompt", DEFAULT_SYSTEM_PROMPT);
            
            // Get user message text
            String userMessage = getStringParam(inputs, "text", "");
            
            // Get image data from inputs
            List<byte[]> imageData = new ArrayList<>();
            if (inputs.containsKey("imageData") && inputs.get("imageData") instanceof byte[]) {
                imageData.add((byte[]) inputs.get("imageData"));
            } else if (inputs.containsKey("imagesData") && inputs.get("imagesData") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> imagesDataList = (List<Object>) inputs.get("imagesData");
                for (Object imgData : imagesDataList) {
                    if (imgData instanceof byte[]) {
                        imageData.add((byte[]) imgData);
                    }
                }
            }
            
            // Get image URLs from inputs
            List<String> imageUrls = new ArrayList<>();
            if (inputs.containsKey("imageUrl") && inputs.get("imageUrl") instanceof String) {
                String url = (String) inputs.get("imageUrl");
                if (!url.isEmpty()) {
                    imageUrls.add(url);
                }
            } else if (inputs.containsKey("imageUrls") && inputs.get("imageUrls") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> urls = (List<String>) inputs.get("imageUrls");
                imageUrls.addAll(urls);
            }
            
            // Get file IDs from inputs
            List<String> fileIds = new ArrayList<>();
            if (inputs.containsKey("fileId") && inputs.get("fileId") instanceof String) {
                String fileId = (String) inputs.get("fileId");
                if (!fileId.isEmpty()) {
                    fileIds.add(fileId);
                }
            } else if (inputs.containsKey("fileIds") && inputs.get("fileIds") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> ids = (List<String>) inputs.get("fileIds");
                fileIds.addAll(ids);
            }
            
            // Check if we have any images
            if (imageData.isEmpty() && imageUrls.isEmpty() && fileIds.isEmpty()) {
                return NodeOutDTO.error("No images provided for multimodal processing");
            }
            
            // If we have file IDs and file service is available, load image data
            if (!fileIds.isEmpty() && fileService.isPresent()) {
                for (String fileId : fileIds) {
                    try {
                        byte[] data = fileService.get().getFileContent(fileId);
                        if (data != null) {
                            imageData.add(data);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load image data for file ID {}: {}", fileId, e.getMessage());
                    }
                }
            }
            
            // Create messages for the multimodal prompt
            List<Message> messages = new ArrayList<>();
            
            // Add system message
            messages.add(new SystemMessage(systemPrompt));
            
            // For direct byte[] image data, create media messages
            List<MediaMessage> mediaMessages = new ArrayList<>();
            for (byte[] imgData : imageData) {
                try {
                    // Create a ByteArrayResource from the image data
                    ByteArrayResource resource = new ByteArrayResource(imgData);
                    
                    // Determine media type based on magic bytes or default to image/jpeg
                    String mediaType = determineMediaType(imgData);
                    
                    // Create a media message
                    MediaMessage mediaMessage = MediaMessage.of(resource, mediaType);
                    mediaMessages.add(mediaMessage);
                } catch (Exception e) {
                    log.warn("Failed to create media message from image data: {}", e.getMessage());
                }
            }
            
            // Create a user message with all the content
            if (!mediaMessages.isEmpty()) {
                // If we have media messages, create a user message with them
                UserMessage userMessageWithMedia = new UserMessage(userMessage, mediaMessages);
                messages.add(userMessageWithMedia);
            } else if (!imageUrls.isEmpty()) {
                // If we have image URLs but no direct data, include them in the user message
                StringBuilder messageWithUrls = new StringBuilder(userMessage);
                messageWithUrls.append("\n\nImage URLs:\n");
                for (String url : imageUrls) {
                    messageWithUrls.append("- ").append(url).append("\n");
                }
                messages.add(new UserMessage(messageWithUrls.toString()));
            } else {
                // Plain text message
                messages.add(new UserMessage(userMessage));
            }
            
            // Set model options
            Map<String, Object> options = new HashMap<>();
            options.put("model", model);
            options.put("maxTokens", 1000);
            
            // Call AI service for multimodal processing
            Prompt prompt = new Prompt(messages);
            log.debug("Sending multimodal processing prompt: {}", prompt);
            
            ChatResponse response = chatClient.call(prompt);
            String result = response.getResult().getOutput().getContent();
            log.debug("Received multimodal processing response: {}", result);
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("response", result);
            outputs.put("text", userMessage);
            outputs.put("imageCount", imageData.size() + imageUrls.size());
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("model", model);
            metadata.put("hasImageData", !imageData.isEmpty());
            metadata.put("hasImageUrls", !imageUrls.isEmpty());
            metadata.put("hasFileIds", !fileIds.isEmpty());
            
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error processing multimodal content: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Multimodal processing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determine the media type based on the image data
     */
    private String determineMediaType(byte[] data) {
        if (data == null || data.length < 4) {
            return "image/jpeg"; // Default
        }
        
        // Check for common image formats by magic bytes
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
            return "image/jpeg"; // JPEG
        } else if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50 && data[2] == (byte) 0x4E && data[3] == (byte) 0x47) {
            return "image/png"; // PNG
        } else if (data[0] == (byte) 0x47 && data[1] == (byte) 0x49 && data[2] == (byte) 0x46) {
            return "image/gif"; // GIF
        } else if (data[0] == (byte) 0x42 && data[1] == (byte) 0x4D) {
            return "image/bmp"; // BMP
        } else if ((data[0] == (byte) 0x49 && data[1] == (byte) 0x49) || (data[0] == (byte) 0x4D && data[1] == (byte) 0x4D)) {
            return "image/tiff"; // TIFF
        } else if (data[0] == (byte) 0x00 && data[1] == (byte) 0x00 && data[2] == (byte) 0x01 && data[3] == (byte) 0x00) {
            return "image/x-icon"; // ICO
        }
        
        // Default to JPEG if unknown
        return "image/jpeg";
    }
    
    /**
     * Helper method to convert Node to NodeDefDTO
     */
    private NodeDefDTO convertToNodeDefDTO(Node node) {
        NodeDefDTO nodeDefDTO = new NodeDefDTO();
        nodeDefDTO.setId(node.getId());
        nodeDefDTO.setType(node.getType());
        nodeDefDTO.setData(node.getData());
        return nodeDefDTO;
    }
    
    /**
     * Helper method to get a string parameter with default value
     */
    private String getStringParam(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
} 