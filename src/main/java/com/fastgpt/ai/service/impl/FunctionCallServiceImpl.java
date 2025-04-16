package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.FunctionCallResult;
import com.fastgpt.ai.dto.FunctionDefinition;
import com.fastgpt.ai.exception.FunctionExecutionException;
import com.fastgpt.ai.exception.InvalidArgumentException;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.service.FunctionCallService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of FunctionCallService
 */
@Slf4j
@Service
public class FunctionCallServiceImpl implements FunctionCallService {
    
    private final Map<String, FunctionDefinition> functionsById = new ConcurrentHashMap<>();
    private final Map<String, FunctionDefinition> functionsByName = new ConcurrentHashMap<>();
    private final Map<String, Function<Map<String, Object>, Object>> functionHandlers = new ConcurrentHashMap<>();
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper;
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    
    @Autowired
    public FunctionCallServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        // Register built-in functions
        registerBuiltInFunctions();
    }
    
    @Override
    public FunctionDefinition registerFunction(FunctionDefinition definition) {
        if (definition.getId() == null || definition.getId().trim().isEmpty()) {
            definition.setId(UUID.randomUUID().toString());
        }
        
        if (definition.getName() == null || definition.getName().trim().isEmpty()) {
            throw new InvalidArgumentException("Function name cannot be empty");
        }
        
        functionsById.put(definition.getId(), definition);
        functionsByName.put(definition.getName(), definition);
        
        log.info("Registered function: {} (ID: {})", definition.getName(), definition.getId());
        
        return definition;
    }
    
    @Override
    public List<FunctionDefinition> registerFunctions(List<FunctionDefinition> definitions) {
        return definitions.stream()
                .map(this::registerFunction)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<FunctionDefinition> getFunction(String functionId) {
        return Optional.ofNullable(functionsById.get(functionId));
    }
    
    @Override
    public Optional<FunctionDefinition> getFunctionByName(String name) {
        return Optional.ofNullable(functionsByName.get(name));
    }
    
    @Override
    public List<FunctionDefinition> getAllFunctions() {
        return new ArrayList<>(functionsById.values());
    }
    
    @Override
    public List<FunctionDefinition> getFunctionsByCategory(String category) {
        return functionsById.values().stream()
                .filter(function -> category.equals(function.getCategory()))
                .collect(Collectors.toList());
    }
    
    @Override
    public FunctionCallResult callFunction(String functionId, Map<String, Object> arguments) {
        FunctionDefinition function = functionsById.get(functionId);
        if (function == null) {
            throw new ResourceNotFoundException("Function", "id", functionId);
        }
        
        return executeFunction(function, arguments);
    }
    
    @Override
    public FunctionCallResult callFunctionByName(String name, Map<String, Object> arguments) {
        FunctionDefinition function = functionsByName.get(name);
        if (function == null) {
            throw new ResourceNotFoundException("Function", "name", name);
        }
        
        return executeFunction(function, arguments);
    }
    
    @Override
    public boolean validateArguments(String functionId, Map<String, Object> arguments) {
        FunctionDefinition function = functionsById.get(functionId);
        if (function == null) {
            return false;
        }
        
        Map<String, Object> parameters = function.getParameters();
        if (parameters == null) {
            return true;
        }
        
        // Simplified validation - in a production system, use a proper JSON Schema validator
        // This assumes parameters map follows JSON Schema format with 'required' and 'properties' fields
        List<String> requiredParams = getRequiredParams(parameters);
        Map<String, Object> properties = getSchemaProperties(parameters);
        
        if (requiredParams != null) {
            for (String requiredParam : requiredParams) {
                if (!arguments.containsKey(requiredParam)) {
                    return false;
                }
            }
        }
        
        if (properties != null) {
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();
                
                if (properties.containsKey(paramName)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propertySchema = (Map<String, Object>) properties.get(paramName);
                    String type = (String) propertySchema.get("type");
                    
                    if (!validateType(paramValue, type)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    @Override
    public boolean unregisterFunction(String functionId) {
        FunctionDefinition function = functionsById.remove(functionId);
        if (function != null) {
            functionsByName.remove(function.getName());
            functionHandlers.remove(functionId);
            log.info("Unregistered function: {} (ID: {})", function.getName(), functionId);
            return true;
        }
        return false;
    }
    
    /**
     * Register a function with a custom handler
     */
    public FunctionDefinition registerFunctionWithHandler(FunctionDefinition definition, Function<Map<String, Object>, Object> handler) {
        FunctionDefinition registered = registerFunction(definition);
        functionHandlers.put(registered.getId(), handler);
        return registered;
    }
    
    /**
     * Execute a function with the given arguments
     */
    private FunctionCallResult executeFunction(FunctionDefinition function, Map<String, Object> arguments) {
        String functionId = function.getId();
        String functionName = function.getName();
        long startTime = System.currentTimeMillis();
        
        // Validate arguments
        if (!validateArguments(functionId, arguments)) {
            return FunctionCallResult.error(
                    functionId,
                    functionName,
                    arguments,
                    "Invalid arguments for function",
                    System.currentTimeMillis() - startTime
            );
        }
        
        // Check if we have a registered handler for this function
        Function<Map<String, Object>, Object> handler = functionHandlers.get(functionId);
        if (handler != null) {
            try {
                Object result = handler.apply(arguments);
                long executionTime = System.currentTimeMillis() - startTime;
                
                return FunctionCallResult.success(
                        functionId,
                        functionName,
                        arguments,
                        result,
                        executionTime
                );
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.error("Error executing function handler: {} ({}): {}", functionName, functionId, e.getMessage(), e);
                
                return FunctionCallResult.error(
                        functionId,
                        functionName,
                        arguments,
                        "Function execution error: " + e.getMessage(),
                        executionTime
                );
            }
        } else {
            // No handler registered, try to execute as JavaScript if sandbox is enabled
            if (Boolean.TRUE.equals(function.getSandbox())) {
                return executeInSandbox(function, arguments, startTime);
            } else {
                return FunctionCallResult.error(
                        functionId,
                        functionName,
                        arguments,
                        "No handler registered for function and sandbox execution is disabled",
                        System.currentTimeMillis() - startTime
                );
            }
        }
    }
    
    /**
     * Execute a function in a JavaScript sandbox
     */
    private FunctionCallResult executeInSandbox(FunctionDefinition function, Map<String, Object> arguments, long startTime) {
        String functionId = function.getId();
        String functionName = function.getName();
        
        // Default timeout is 5 seconds if not specified
        long timeoutMs = function.getTimeoutMs() != null ? function.getTimeoutMs() : 5000;
        
        Future<Object> future = executorService.submit(() -> {
            try {
                ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
                if (engine == null) {
                    throw new FunctionExecutionException("JavaScript engine not available");
                }
                
                // Convert arguments to JSON and bind to engine
                String argsJson = objectMapper.writeValueAsString(arguments);
                engine.put("args", engine.eval("JSON.parse('" + argsJson.replace("'", "\\'") + "')"));
                
                // Create and execute the function
                String functionCode = "function " + functionName + "(args) { /* User code here */ }";
                engine.eval(functionCode);
                
                // Call the function
                return engine.eval(functionName + "(args)");
            } catch (Exception e) {
                throw new FunctionExecutionException("Error executing function in sandbox: " + e.getMessage(), e);
            }
        });
        
        try {
            Object result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return FunctionCallResult.success(
                    functionId,
                    functionName,
                    arguments,
                    result,
                    executionTime
            );
        } catch (TimeoutException e) {
            future.cancel(true);
            return FunctionCallResult.error(
                    functionId,
                    functionName,
                    arguments,
                    "Function execution timed out after " + timeoutMs + "ms",
                    timeoutMs
            );
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String errorMessage = cause instanceof FunctionExecutionException
                    ? cause.getMessage()
                    : "Function execution error: " + e.getMessage();
            
            long executionTime = System.currentTimeMillis() - startTime;
            return FunctionCallResult.error(
                    functionId,
                    functionName,
                    arguments,
                    errorMessage,
                    executionTime
            );
        }
    }
    
    /**
     * Register built-in utility functions
     */
    private void registerBuiltInFunctions() {
        // Math functions
        registerMathFunctions();
        
        // String functions
        registerStringFunctions();
        
        // Date/time functions
        registerDateTimeFunctions();
        
        // Utility functions
        registerUtilityFunctions();
    }
    
    /**
     * Register built-in math functions
     */
    private void registerMathFunctions() {
        // Sum function
        FunctionDefinition sumFunction = FunctionDefinition.builder()
                .id("math-sum")
                .name("sum")
                .description("Calculates the sum of numbers")
                .category("math")
                .parameters(createParameterSchema(
                        Map.of(
                                "numbers", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "number"),
                                        "description", "Array of numbers to sum"
                                )
                        ),
                        List.of("numbers")
                ))
                .returnType("number")
                .build();
        
        registerFunctionWithHandler(sumFunction, args -> {
            @SuppressWarnings("unchecked")
            List<Number> numbers = (List<Number>) args.get("numbers");
            return numbers.stream()
                    .mapToDouble(Number::doubleValue)
                    .sum();
        });
        
        // Other math functions can be registered here
    }
    
    /**
     * Register built-in string functions
     */
    private void registerStringFunctions() {
        // String concat function
        FunctionDefinition concatFunction = FunctionDefinition.builder()
                .id("string-concat")
                .name("concat")
                .description("Concatenates multiple strings")
                .category("string")
                .parameters(createParameterSchema(
                        Map.of(
                                "strings", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "string"),
                                        "description", "Array of strings to concatenate"
                                ),
                                "separator", Map.of(
                                        "type", "string",
                                        "description", "Separator between strings",
                                        "default", ""
                                )
                        ),
                        List.of("strings")
                ))
                .returnType("string")
                .build();
        
        registerFunctionWithHandler(concatFunction, args -> {
            @SuppressWarnings("unchecked")
            List<String> strings = (List<String>) args.get("strings");
            String separator = args.containsKey("separator") ? (String) args.get("separator") : "";
            return String.join(separator, strings);
        });
        
        // Other string functions can be registered here
    }
    
    /**
     * Register built-in date/time functions
     */
    private void registerDateTimeFunctions() {
        // Current date/time function
        FunctionDefinition nowFunction = FunctionDefinition.builder()
                .id("datetime-now")
                .name("now")
                .description("Returns the current date and time")
                .category("datetime")
                .parameters(createParameterSchema(
                        Map.of(
                                "format", Map.of(
                                        "type", "string",
                                        "description", "Output format (iso, timestamp)",
                                        "enum", List.of("iso", "timestamp"),
                                        "default", "iso"
                                )
                        ),
                        List.of()
                ))
                .returnType("string")
                .build();
        
        registerFunctionWithHandler(nowFunction, args -> {
            String format = args.containsKey("format") ? (String) args.get("format") : "iso";
            LocalDateTime now = LocalDateTime.now();
            
            if ("timestamp".equals(format)) {
                return now.toString();
            } else {
                return now.toString();
            }
        });
        
        // Other date/time functions can be registered here
    }
    
    /**
     * Register built-in utility functions
     */
    private void registerUtilityFunctions() {
        // Random number function
        FunctionDefinition randomFunction = FunctionDefinition.builder()
                .id("utility-random")
                .name("random")
                .description("Generates a random number")
                .category("utility")
                .parameters(createParameterSchema(
                        Map.of(
                                "min", Map.of(
                                        "type", "number",
                                        "description", "Minimum value (inclusive)",
                                        "default", 0
                                ),
                                "max", Map.of(
                                        "type", "number",
                                        "description", "Maximum value (exclusive)",
                                        "default", 1
                                )
                        ),
                        List.of()
                ))
                .returnType("number")
                .build();
        
        registerFunctionWithHandler(randomFunction, args -> {
            double min = args.containsKey("min") ? ((Number) args.get("min")).doubleValue() : 0;
            double max = args.containsKey("max") ? ((Number) args.get("max")).doubleValue() : 1;
            return min + Math.random() * (max - min);
        });
        
        // Other utility functions can be registered here
    }
    
    /**
     * Helper to create a JSON Schema for function parameters
     */
    private Map<String, Object> createParameterSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }
    
    /**
     * Get required parameters from a JSON Schema
     */
    @SuppressWarnings("unchecked")
    private List<String> getRequiredParams(Map<String, Object> schema) {
        Object required = schema.get("required");
        if (required instanceof List) {
            return (List<String>) required;
        }
        return Collections.emptyList();
    }
    
    /**
     * Get properties from a JSON Schema
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getSchemaProperties(Map<String, Object> schema) {
        Object properties = schema.get("properties");
        if (properties instanceof Map) {
            return (Map<String, Object>) properties;
        }
        return Collections.emptyMap();
    }
    
    /**
     * Validate a value against a JSON Schema type
     */
    private boolean validateType(Object value, String type) {
        if (value == null) {
            return true; // Null is valid for any type unless there's a "nullable": false constraint
        }
        
        switch (type) {
            case "string":
                return value instanceof String;
            case "number":
                return value instanceof Number;
            case "integer":
                return value instanceof Integer || value instanceof Long;
            case "boolean":
                return value instanceof Boolean;
            case "array":
                return value instanceof List || value.getClass().isArray();
            case "object":
                return value instanceof Map;
            default:
                return true; // Unknown type, assume valid
        }
    }
} 