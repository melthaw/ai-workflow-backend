package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fastgpt.ai.dto.openai.ChatMessage;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node dispatcher for managing conversation history and context
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationManagerNodeDispatcher implements NodeDispatcher {

    private static final String NODE_TYPE = "ai.conversation.manager";
    
    // Default parameters
    private static final int DEFAULT_MAX_MESSAGES = 10;
    private static final int DEFAULT_MAX_TOKENS = 4000;
    private static final boolean DEFAULT_INCLUDE_SYSTEM = true;
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return manageConversation(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in conversation manager node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Conversation management failed: " + e.getMessage());
        }
    }
    
    /**
     * Process conversation management based on inputs
     */
    private NodeOutDTO manageConversation(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing conversation manager node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get configuration parameters
            int maxMessages = getIntParam(nodeData, "maxMessages", DEFAULT_MAX_MESSAGES);
            int maxTokens = getIntParam(nodeData, "maxTokens", DEFAULT_MAX_TOKENS);
            boolean includeSystem = getBooleanParam(nodeData, "includeSystem", DEFAULT_INCLUDE_SYSTEM);
            String summaryStrategy = getStringParam(nodeData, "summaryStrategy", "truncate");
            
            // Get input message and history
            String userMessage = getStringParam(inputs, "message", "");
            String systemMessage = getStringParam(inputs, "systemMessage", "");
            
            @SuppressWarnings("unchecked")
            List<ChatMessage> history = inputs.get("history") instanceof List 
                ? (List<ChatMessage>) inputs.get("history") 
                : new ArrayList<>();
            
            // Create new history if it doesn't exist
            if (history == null) {
                history = new ArrayList<>();
            }
            
            // Add system message if required and not already present
            if (includeSystem && !systemMessage.isEmpty() && 
                (history.isEmpty() || !isSystemMessagePresent(history))) {
                ChatMessage sysMsg = new ChatMessage();
                sysMsg.setRole("system");
                sysMsg.setContent(systemMessage);
                history.add(0, sysMsg);
            }
            
            // Add user message to history if provided
            if (!userMessage.isEmpty()) {
                ChatMessage msg = new ChatMessage();
                msg.setRole("user");
                msg.setContent(userMessage);
                history.add(msg);
            }
            
            // Apply conversation management strategy
            List<ChatMessage> managedHistory = manageConversationHistory(
                history, maxMessages, maxTokens, summaryStrategy);
            
            // Calculate approximate token count
            int approximateTokens = calculateApproximateTokens(managedHistory);
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("history", managedHistory);
            outputs.put("messageCount", managedHistory.size());
            outputs.put("approximateTokens", approximateTokens);
            
            // Extract the most recent messages for convenience
            if (!managedHistory.isEmpty()) {
                ChatMessage lastMessage = managedHistory.get(managedHistory.size() - 1);
                outputs.put("lastMessage", lastMessage);
                outputs.put("lastUserMessage", findLastMessageOfRole(managedHistory, "user"));
                outputs.put("lastAssistantMessage", findLastMessageOfRole(managedHistory, "assistant"));
            }
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("maxMessages", maxMessages);
            metadata.put("maxTokens", maxTokens);
            metadata.put("strategy", summaryStrategy);
            
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error managing conversation: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Conversation management failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Apply management strategy to conversation history
     */
    private List<ChatMessage> manageConversationHistory(
            List<ChatMessage> history, 
            int maxMessages, 
            int maxTokens,
            String strategy) {
        
        // Don't modify history if within limits
        if (history.size() <= maxMessages && 
            calculateApproximateTokens(history) <= maxTokens) {
            return new ArrayList<>(history);
        }
        
        List<ChatMessage> result = new ArrayList<>(history);
        
        switch (strategy.toLowerCase()) {
            case "summarize":
                // Not implemented yet - would require LLM call to summarize earlier conversation
                // For now, fallback to truncation
                return truncateHistory(result, maxMessages, maxTokens);
                
            case "window":
                // Keep a sliding window of the most recent messages
                return windowHistory(result, maxMessages, maxTokens);
                
            case "truncate":
            default:
                // Remove oldest messages first, but preserve system messages if possible
                return truncateHistory(result, maxMessages, maxTokens);
        }
    }
    
    /**
     * Truncate history by removing oldest messages (except system messages if possible)
     */
    private List<ChatMessage> truncateHistory(
            List<ChatMessage> history, 
            int maxMessages, 
            int maxTokens) {
        
        List<ChatMessage> result = new ArrayList<>(history);
        
        // First, try to reduce to max messages
        while (result.size() > maxMessages) {
            // Find oldest non-system message to remove
            int indexToRemove = findOldestNonSystemMessageIndex(result);
            if (indexToRemove >= 0) {
                result.remove(indexToRemove);
            } else {
                // No non-system messages left, remove oldest message
                result.remove(0);
            }
        }
        
        // Then, check token limit
        while (calculateApproximateTokens(result) > maxTokens && !result.isEmpty()) {
            // Find oldest non-system message to remove
            int indexToRemove = findOldestNonSystemMessageIndex(result);
            if (indexToRemove >= 0) {
                result.remove(indexToRemove);
            } else {
                // No non-system messages left, remove oldest message
                result.remove(0);
            }
        }
        
        return result;
    }
    
    /**
     * Keep a sliding window of the most recent messages
     */
    private List<ChatMessage> windowHistory(
            List<ChatMessage> history, 
            int maxMessages, 
            int maxTokens) {
        
        List<ChatMessage> result = new ArrayList<>();
        
        // Always keep system messages if present
        for (ChatMessage msg : history) {
            if ("system".equals(msg.getRole())) {
                result.add(msg);
            }
        }
        
        // Add most recent messages until limits are reached
        List<ChatMessage> nonSystemMessages = history.stream()
            .filter(msg -> !"system".equals(msg.getRole()))
            .toList();
        
        int remaining = Math.min(
            maxMessages - result.size(),
            nonSystemMessages.size()
        );
        
        // Add most recent messages up to the remaining count
        if (remaining > 0) {
            for (int i = nonSystemMessages.size() - remaining; i < nonSystemMessages.size(); i++) {
                result.add(nonSystemMessages.get(i));
            }
        }
        
        // Check token limit and remove older messages if needed
        while (calculateApproximateTokens(result) > maxTokens && !result.isEmpty()) {
            // Find oldest non-system message to remove
            int indexToRemove = findOldestNonSystemMessageIndex(result);
            if (indexToRemove >= 0) {
                result.remove(indexToRemove);
            } else {
                // No non-system messages left, stop
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Calculate approximate token count for the conversation
     * Uses a very rough approximation of 4 characters per token
     */
    private int calculateApproximateTokens(List<ChatMessage> history) {
        // Simple approximation: 1 token ≈ 4 characters
        // This is not accurate but gives a rough estimation
        int characterCount = 0;
        
        for (ChatMessage message : history) {
            // Add role overhead (approximately 4 tokens per message)
            characterCount += 16;
            
            // Add content
            if (message.getContent() != null) {
                characterCount += message.getContent().length();
            }
        }
        
        return characterCount / 4;
    }
    
    /**
     * Find the index of the oldest non-system message
     */
    private int findOldestNonSystemMessageIndex(List<ChatMessage> messages) {
        for (int i = 0; i < messages.size(); i++) {
            if (!"system".equals(messages.get(i).getRole())) {
                return i;
            }
        }
        return -1; // No non-system messages found
    }
    
    /**
     * Check if a system message is already present in the history
     */
    private boolean isSystemMessagePresent(List<ChatMessage> history) {
        return history.stream().anyMatch(msg -> "system".equals(msg.getRole()));
    }
    
    /**
     * Find the last message with a specific role
     */
    private ChatMessage findLastMessageOfRole(List<ChatMessage> history, String role) {
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if (role.equals(msg.getRole())) {
                return msg;
            }
        }
        return null;
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
    
    /**
     * Helper method to get a boolean parameter with default value
     */
    private Boolean getBooleanParam(Map<String, Object> data, String key, Boolean defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
} 