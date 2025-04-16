package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fastgpt.ai.dto.openai.ChatMessage;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node dispatcher for text generation using AI models
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextGenerationNodeDispatcher implements NodeDispatcher {

    private final ChatClient chatClient;
    
    private static final String NODE_TYPE = "ai.text.generation";
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return generateText(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in text generation node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Text generation failed: " + e.getMessage());
        }
    }
    
    /**
     * Process text generation based on inputs
     */
    private NodeOutDTO generateText(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing text generation node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get required parameters
            String systemPrompt = getStringParam(nodeData, "systemPrompt", "You are a helpful assistant.");
            String promptTemplate = getStringParam(nodeData, "promptTemplate", "");
            Double temperature = getDoubleParam(nodeData, "temperature", 0.7);
            Double topP = getDoubleParam(nodeData, "topP", 1.0);
            Integer maxTokens = getIntParam(nodeData, "maxTokens", 1000);
            
            // Prepare messages for the AI model
            List<Message> messages = new ArrayList<>();
            
            // Add system message if provided
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            
            // Process the prompt template if available
            String userContent;
            if (promptTemplate != null && !promptTemplate.trim().isEmpty()) {
                // Use Spring AI's PromptTemplate for variable substitution
                PromptTemplate template = new PromptTemplate(promptTemplate);
                userContent = template.render(inputs);
            } else {
                // Fallback to direct input text if available
                userContent = getStringParam(inputs, "text", "");
            }
            
            // Add user message
            messages.add(new UserMessage(userContent));
            
            // Include conversation history if available
            @SuppressWarnings("unchecked")
            List<ChatMessage> history = (List<ChatMessage>) inputs.get("history");
            if (history != null) {
                for (ChatMessage chatMessage : history) {
                    if ("system".equals(chatMessage.getRole())) {
                        messages.add(new SystemMessage(chatMessage.getContent()));
                    } else if ("user".equals(chatMessage.getRole())) {
                        messages.add(new UserMessage(chatMessage.getContent()));
                    } else if ("assistant".equals(chatMessage.getRole())) {
                        messages.add(new org.springframework.ai.chat.messages.AssistantMessage(chatMessage.getContent()));
                    }
                }
            }
            
            // Create the prompt with model parameters
            Prompt prompt = new Prompt(messages);
            
            // Call the AI model
            ChatResponse response = chatClient.call(prompt);
            
            // Extract the generated text
            String generatedText = response.getResult().getOutput().getContent();
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("text", generatedText);
            
            // Add metadata to the output
            Map<String, Object> metadata = new HashMap<>();
            
            // Add some basic metadata
            metadata.put("model", "AI model");
            metadata.put("timestamp", System.currentTimeMillis());
            
            // Add the outputs and metadata to the result
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error generating text: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Text generation failed: " + e.getMessage(), e);
        }
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
    
    /**
     * Helper method to get a double parameter with default value
     */
    private Double getDoubleParam(Map<String, Object> data, String key, Double defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Helper method to get an integer parameter with default value
     */
    private Integer getIntParam(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
} 