package com.fastgpt.ai.service.function.impl;

import com.fastgpt.ai.dto.function.FunctionCallResponse;
import com.fastgpt.ai.dto.function.FunctionDefinition;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.function.FunctionCallingService;
import com.fastgpt.ai.service.function.FunctionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of FunctionCallingService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallingServiceImpl implements FunctionCallingService {
    
    @Value("${app.function-calling.default-model:gpt-4}")
    private String defaultModel;
    
    @Value("${app.function-calling.max-retries:3}")
    private int maxRetries;
    
    private final AiService aiService;
    
    // Maps to store function definitions and executors
    private final Map<String, FunctionDefinition> functions = new ConcurrentHashMap<>();
    private final Map<String, FunctionExecutor> executors = new ConcurrentHashMap<>();
    private final Map<String, FunctionDefinition> coreFunctions = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // Register built-in functions
        registerCurrentTimeFunction();
        registerWeatherFunction();
        
        log.info("Function calling service initialized with {} built-in functions", coreFunctions.size());
    }
    
    @Override
    public void registerFunction(FunctionDefinition definition, FunctionExecutor executor) {
        String name = definition.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Function name cannot be empty");
        }
        
        functions.put(name, definition);
        executors.put(name, executor);
        
        log.debug("Registered function: {}", name);
    }
    
    @Override
    public void registerCoreFunction(String name, FunctionDefinition definition, FunctionExecutor executor) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Function name cannot be empty");
        }
        
        coreFunctions.put(name, definition);
        functions.put(name, definition);
        executors.put(name, executor);
        
        log.debug("Registered core function: {}", name);
    }
    
    @Override
    public FunctionDefinition getFunctionDefinition(String name) {
        return functions.get(name);
    }
    
    @Override
    public FunctionExecutor getFunctionExecutor(String name) {
        return executors.get(name);
    }
    
    @Override
    public CompletableFuture<String> callWithFunctions(String prompt, List<String> functionNames, String model) {
        // Get the requested function definitions
        List<FunctionDefinition> availableFunctions = functionNames.stream()
                .map(functions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (availableFunctions.isEmpty()) {
            log.warn("No valid functions found for names: {}", functionNames);
        }
        
        // Call AI service with functions
        return aiService.generateWithFunctions(prompt, availableFunctions, model)
                .thenCompose(this::processFunctionCalls);
    }
    
    @Override
    public CompletableFuture<String> callWithFunctions(String prompt, List<String> functionNames) {
        return callWithFunctions(prompt, functionNames, defaultModel);
    }
    
    @Override
    public CompletableFuture<String> callWithAllFunctions(String prompt) {
        return callWithFunctions(prompt, new ArrayList<>(functions.keySet()));
    }
    
    @Override
    public CompletableFuture<String> processFunctionCalls(FunctionCallResponse response) {
        if (!response.hasFunctionCalls()) {
            // No functions to call, return the text response
            return CompletableFuture.completedFuture(response.getTextResponse());
        }
        
        // Get the first function call
        FunctionCallResponse.FunctionCall functionCall = response.getFunctionCalls().get(0);
        String functionName = functionCall.getName();
        
        // Find the executor
        FunctionExecutor executor = executors.get(functionName);
        if (executor == null) {
            log.warn("No executor found for function: {}", functionName);
            return CompletableFuture.completedFuture(
                    "Error: Function '" + functionName + "' not found. " + response.getTextResponse());
        }
        
        // Execute the function
        return executor.execute(functionCall)
                .thenCompose(result -> {
                    // Format the function result as a message
                    String functionResult = formatFunctionResult(functionName, result);
                    
                    // Process any additional function calls recursively
                    if (response.getFunctionCalls().size() > 1) {
                        FunctionCallResponse remainingCalls = FunctionCallResponse.builder()
                                .functionCalls(response.getFunctionCalls().subList(1, response.getFunctionCalls().size()))
                                .textResponse(response.getTextResponse())
                                .build();
                        
                        return processFunctionCalls(remainingCalls)
                                .thenApply(nextResult -> functionResult + "\n" + nextResult);
                    }
                    
                    return CompletableFuture.completedFuture(functionResult);
                })
                .exceptionally(ex -> {
                    log.error("Error executing function {}: {}", functionName, ex.getMessage());
                    return "Error executing function '" + functionName + "': " + ex.getMessage();
                });
    }
    
    @Override
    public Map<String, FunctionDefinition> getAllFunctions() {
        return Collections.unmodifiableMap(functions);
    }
    
    @Override
    public Map<String, FunctionDefinition> getCoreFunctions() {
        return Collections.unmodifiableMap(coreFunctions);
    }
    
    // Helper methods
    
    private String formatFunctionResult(String functionName, Object result) {
        if (result == null) {
            return "Function '" + functionName + "' executed successfully but returned no result.";
        }
        
        return result.toString();
    }
    
    // Demo function implementations
    
    private void registerCurrentTimeFunction() {
        FunctionDefinition definition = FunctionDefinition.builder()
                .name("getCurrentTime")
                .description("Get the current date and time")
                .parameters(Map.of(
                        "format", Map.of(
                                "type", "string",
                                "description", "Optional format for the date/time (e.g., 'yyyy-MM-dd HH:mm:ss')",
                                "required", false
                        )
                ))
                .build();
        
        FunctionExecutor executor = FunctionExecutor.of(args -> {
            String format = (String) args.getOrDefault("format", "yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            
            return "The current time is: " + LocalDateTime.now().format(formatter);
        });
        
        registerCoreFunction("getCurrentTime", definition, executor);
    }
    
    private void registerWeatherFunction() {
        FunctionDefinition definition = FunctionDefinition.builder()
                .name("getWeather")
                .description("Get the current weather for a location")
                .parameters(Map.of(
                        "location", Map.of(
                                "type", "string",
                                "description", "The city or location to get weather for",
                                "required", true
                        ),
                        "unit", Map.of(
                                "type", "string",
                                "description", "The unit of temperature (celsius or fahrenheit)",
                                "enum", List.of("celsius", "fahrenheit"),
                                "required", false
                        )
                ))
                .build();
        
        FunctionExecutor executor = FunctionExecutor.ofAsync(args -> {
            String location = (String) args.get("location");
            String unit = (String) args.getOrDefault("unit", "celsius");
            
            // In a real implementation, this would call a weather API
            // For demo purposes, return mock data
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate API delay
                    Thread.sleep(500);
                    
                    // Generate random temperature between 0-30°C or 32-86°F
                    Random random = new Random();
                    int temp;
                    String unitSymbol;
                    
                    if ("fahrenheit".equals(unit)) {
                        temp = random.nextInt(54) + 32;
                        unitSymbol = "°F";
                    } else {
                        temp = random.nextInt(30);
                        unitSymbol = "°C";
                    }
                    
                    // Random weather conditions
                    String[] conditions = {"Sunny", "Partly Cloudy", "Cloudy", "Rainy", "Stormy", "Snowy"};
                    String condition = conditions[random.nextInt(conditions.length)];
                    
                    return String.format("Weather for %s: %s, Temperature: %d%s", 
                            location, condition, temp, unitSymbol);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Error retrieving weather data: " + e.getMessage();
                }
            });
        });
        
        registerCoreFunction("getWeather", definition, executor);
    }
} 