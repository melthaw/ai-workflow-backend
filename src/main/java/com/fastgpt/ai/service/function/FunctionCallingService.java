package com.fastgpt.ai.service.function;

import com.fastgpt.ai.dto.function.FunctionCallResponse;
import com.fastgpt.ai.dto.function.FunctionDefinition;
import com.fastgpt.ai.dto.openai.ChatCompletionRequest;
import com.fastgpt.ai.dto.openai.ChatMessage;
import com.fastgpt.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing and executing AI function calls
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallingService {
    
    private final AiService aiService;
    private final List<FunctionProvider> functionProviders;
    
    @Value("${spring.ai.openai.model:gpt-3.5-turbo}")
    private String defaultModel;
    
    /**
     * Get all available functions from all providers
     * @return Map of function definitions by name
     */
    public Map<String, FunctionDefinition> getAllFunctions() {
        Map<String, FunctionDefinition> allFunctions = new HashMap<>();
        
        for (FunctionProvider provider : functionProviders) {
            for (FunctionDefinition function : provider.getFunctions()) {
                allFunctions.put(function.getName(), function);
            }
        }
        
        return allFunctions;
    }
    
    /**
     * Get core system functions
     * @return Map of core function definitions by name
     */
    public Map<String, FunctionDefinition> getCoreFunctions() {
        Map<String, FunctionDefinition> coreFunctions = new HashMap<>();
        
        for (FunctionProvider provider : functionProviders) {
            if (provider.isCore()) {
                for (FunctionDefinition function : provider.getFunctions()) {
                    coreFunctions.put(function.getName(), function);
                }
            }
        }
        
        return coreFunctions;
    }
    
    /**
     * Get a specific function definition by name
     * @param name Function name
     * @return Function definition or null if not found
     */
    public FunctionDefinition getFunctionDefinition(String name) {
        for (FunctionProvider provider : functionProviders) {
            for (FunctionDefinition function : provider.getFunctions()) {
                if (function.getName().equals(name)) {
                    return function;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Execute a function by name with provided parameters
     * @param name Function name
     * @param parameters Function parameters
     * @return Function execution result
     */
    public Map<String, Object> executeFunction(String name, Map<String, Object> parameters) {
        log.info("Executing function: {} with parameters: {}", name, parameters);
        
        for (FunctionProvider provider : functionProviders) {
            for (FunctionDefinition function : provider.getFunctions()) {
                if (function.getName().equals(name)) {
                    return provider.executeFunction(name, parameters);
                }
            }
        }
        
        throw new IllegalArgumentException("Function not found: " + name);
    }
    
    /**
     * Generate AI response with selected functions
     * @param prompt User prompt
     * @param functionNames List of function names to include
     * @return CompletableFuture containing the response
     */
    public CompletableFuture<String> callWithFunctions(String prompt, List<String> functionNames) {
        return callWithFunctions(prompt, functionNames, defaultModel);
    }
    
    /**
     * Generate AI response with selected functions and specified model
     * @param prompt User prompt
     * @param functionNames List of function names to include
     * @param model Model to use
     * @return CompletableFuture containing the response
     */
    public CompletableFuture<String> callWithFunctions(String prompt, List<String> functionNames, String model) {
        List<FunctionDefinition> functionDefinitions = prepareFunctions(functionNames);
        
        return aiService.generateWithFunctions(prompt, functionDefinitions, model)
                .thenCompose(response -> processFunctionCallResponse(response, prompt));
    }
    
    /**
     * Generate AI response with all available functions
     * @param prompt User prompt
     * @return CompletableFuture containing the response
     */
    public CompletableFuture<String> callWithAllFunctions(String prompt) {
        List<FunctionDefinition> allFunctions = new ArrayList<>(getAllFunctions().values());
        
        return aiService.generateWithFunctions(prompt, allFunctions)
                .thenCompose(response -> processFunctionCallResponse(response, prompt));
    }
    
    /**
     * Prepare function definitions from a list of function names
     * @param functionNames List of function names
     * @return List of function definitions
     */
    private List<FunctionDefinition> prepareFunctions(List<String> functionNames) {
        Map<String, FunctionDefinition> allFunctions = getAllFunctions();
        
        return functionNames.stream()
                .filter(allFunctions::containsKey)
                .map(allFunctions::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Process a function call response
     * @param response Function call response
     * @param originalPrompt Original user prompt
     * @return CompletableFuture containing the final response
     */
    private CompletableFuture<String> processFunctionCallResponse(FunctionCallResponse response, String originalPrompt) {
        // If no function call, just return the content
        if (response.getFunctionCall() == null) {
            return CompletableFuture.completedFuture(response.getContent());
        }
        
        FunctionCallResponse.FunctionCall functionCall = response.getFunctionCall();
        
        try {
            // Execute the function
            Map<String, Object> result = executeFunction(
                    functionCall.getName(),
                    functionCall.getArguments()
            );
            
            // Continue conversation with function result
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("user", originalPrompt));
            messages.add(new ChatMessage("assistant", response.getContent()));
            
            // Add function call and result as a tool call
            ChatMessage toolMessage = new ChatMessage("function", 
                    result.containsKey("result") ? result.get("result").toString() : "Function executed successfully");
            toolMessage.setName(functionCall.getName());
            messages.add(toolMessage);
            
            // Ask AI to interpret the result
            ChatCompletionRequest request = new ChatCompletionRequest();
            request.setModel(defaultModel);
            request.setMessages(messages);
            
            return aiService.generateResponse(originalPrompt, null, defaultModel, 0.7, 1000);
        } catch (Exception e) {
            log.error("Error executing function: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    "Error executing function " + functionCall.getName() + ": " + e.getMessage()
            );
        }
    }
} 