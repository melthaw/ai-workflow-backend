package com.fastgpt.ai.dto.openai;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for OpenAI chat completion API
 */
public class ChatCompletionRequest {
    private String model;
    private List<ChatMessage> messages;
    private Float temperature;
    private Integer maxTokens;
    private Float topP;
    private Float presencePenalty;
    private Float frequencyPenalty;
    private Boolean stream;
    private List<Map<String, Object>> functions;
    private String functionCall;
    
    /**
     * Default constructor
     */
    public ChatCompletionRequest() {
    }
    
    /**
     * Constructor with essential parameters
     */
    public ChatCompletionRequest(String model, List<ChatMessage> messages) {
        this.model = model;
        this.messages = messages;
    }
    
    // Getters and setters
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public Float getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Float getTopP() {
        return topP;
    }
    
    public void setTopP(Float topP) {
        this.topP = topP;
    }
    
    public Float getPresencePenalty() {
        return presencePenalty;
    }
    
    public void setPresencePenalty(Float presencePenalty) {
        this.presencePenalty = presencePenalty;
    }
    
    public Float getFrequencyPenalty() {
        return frequencyPenalty;
    }
    
    public void setFrequencyPenalty(Float frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
    
    public List<Map<String, Object>> getFunctions() {
        return functions;
    }
    
    public void setFunctions(List<Map<String, Object>> functions) {
        this.functions = functions;
    }
    
    public String getFunctionCall() {
        return functionCall;
    }
    
    public void setFunctionCall(String functionCall) {
        this.functionCall = functionCall;
    }
} 