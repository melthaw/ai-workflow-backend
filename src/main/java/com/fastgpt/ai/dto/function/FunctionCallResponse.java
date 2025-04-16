package com.fastgpt.ai.dto.function;

import com.fastgpt.ai.dto.openai.OpenAIResponse;

import java.util.Map;

/**
 * Represents a response with potential function call
 */
public class FunctionCallResponse {
    private String content;
    private FunctionCall functionCall;
    private OpenAIResponse rawResponse;
    
    public FunctionCallResponse() {
    }
    
    public FunctionCallResponse(String content, FunctionCall functionCall, OpenAIResponse rawResponse) {
        this.content = content;
        this.functionCall = functionCall;
        this.rawResponse = rawResponse;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public FunctionCall getFunctionCall() {
        return functionCall;
    }
    
    public void setFunctionCall(FunctionCall functionCall) {
        this.functionCall = functionCall;
    }
    
    public OpenAIResponse getRawResponse() {
        return rawResponse;
    }
    
    public void setRawResponse(OpenAIResponse rawResponse) {
        this.rawResponse = rawResponse;
    }
    
    /**
     * Represents a function call from the model
     */
    public static class FunctionCall {
        private String name;
        private Map<String, Object> arguments;
        private String rawArguments;
        
        public FunctionCall() {
        }
        
        public FunctionCall(String name, Map<String, Object> arguments, String rawArguments) {
            this.name = name;
            this.arguments = arguments;
            this.rawArguments = rawArguments;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Map<String, Object> getArguments() {
            return arguments;
        }
        
        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }
        
        public String getRawArguments() {
            return rawArguments;
        }
        
        public void setRawArguments(String rawArguments) {
            this.rawArguments = rawArguments;
        }
    }
} 