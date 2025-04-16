package com.fastgpt.ai.service.function;

import com.fastgpt.ai.dto.function.FunctionDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Provides core system functions
 */
@Slf4j
@Component
public class CoreFunctionProvider implements FunctionProvider {
    
    private final List<FunctionDefinition> functions;
    
    public CoreFunctionProvider() {
        this.functions = buildCoreFunctions();
    }
    
    @Override
    public List<FunctionDefinition> getFunctions() {
        return functions;
    }
    
    @Override
    public Map<String, Object> executeFunction(String name, Map<String, Object> parameters) {
        log.info("Executing core function: {} with parameters: {}", name, parameters);
        
        return switch (name) {
            case "get_current_time" -> getCurrentTime(parameters);
            case "get_random_number" -> getRandomNumber(parameters);
            case "calculate_expression" -> calculateExpression(parameters);
            case "format_text" -> formatText(parameters);
            default -> throw new IllegalArgumentException("Unknown function: " + name);
        };
    }
    
    @Override
    public boolean isCore() {
        return true;
    }
    
    /**
     * Build the list of core functions
     */
    private List<FunctionDefinition> buildCoreFunctions() {
        List<FunctionDefinition> functionList = new ArrayList<>();
        
        // Get current time function
        functionList.add(FunctionDefinition.builder()
                .name("get_current_time")
                .description("Get the current date and time, optionally in a specific timezone")
                .parameters(Map.of(
                        "timezone", Map.of(
                                "type", "string",
                                "description", "Timezone (e.g. UTC, GMT, America/New_York)",
                                "required", false
                        ),
                        "format", Map.of(
                                "type", "string",
                                "description", "Date/time format string",
                                "required", false
                        )
                ))
                .build());
        
        // Random number generator
        functionList.add(FunctionDefinition.builder()
                .name("get_random_number")
                .description("Generate a random number within a specified range")
                .parameters(Map.of(
                        "min", Map.of(
                                "type", "number",
                                "description", "Minimum value (inclusive)",
                                "required", true
                        ),
                        "max", Map.of(
                                "type", "number",
                                "description", "Maximum value (inclusive)",
                                "required", true
                        )
                ))
                .build());
        
        // Simple calculator
        functionList.add(FunctionDefinition.builder()
                .name("calculate_expression")
                .description("Evaluate a mathematical expression")
                .parameters(Map.of(
                        "expression", Map.of(
                                "type", "string",
                                "description", "Mathematical expression to evaluate",
                                "required", true
                        )
                ))
                .build());
        
        // Text formatter
        functionList.add(FunctionDefinition.builder()
                .name("format_text")
                .description("Format text with specified options")
                .parameters(Map.of(
                        "text", Map.of(
                                "type", "string",
                                "description", "Text to format",
                                "required", true
                        ),
                        "operation", Map.of(
                                "type", "string",
                                "description", "Formatting operation (upper, lower, capitalize, trim)",
                                "enum", List.of("upper", "lower", "capitalize", "trim"),
                                "required", true
                        )
                ))
                .build());
        
        return functionList;
    }
    
    /**
     * Get current time with optional formatting and timezone
     */
    private Map<String, Object> getCurrentTime(Map<String, Object> parameters) {
        String timezone = (String) parameters.getOrDefault("timezone", "UTC");
        String format = (String) parameters.getOrDefault("format", "yyyy-MM-dd HH:mm:ss");
        
        ZoneOffset zoneOffset;
        try {
            zoneOffset = ZoneOffset.of(timezone);
        } catch (Exception e) {
            zoneOffset = ZoneOffset.UTC;
        }
        
        LocalDateTime now = LocalDateTime.now(zoneOffset);
        String formattedTime = now.format(DateTimeFormatter.ofPattern(format));
        
        return Map.of(
            "result", formattedTime,
            "timestamp", now.toEpochSecond(zoneOffset),
            "timezone", timezone
        );
    }
    
    /**
     * Generate a random number in a specified range
     */
    private Map<String, Object> getRandomNumber(Map<String, Object> parameters) {
        double min = ((Number) parameters.get("min")).doubleValue();
        double max = ((Number) parameters.get("max")).doubleValue();
        
        if (min > max) {
            throw new IllegalArgumentException("Minimum value must be less than or equal to maximum value");
        }
        
        Random random = new Random();
        double result = min + (max - min) * random.nextDouble();
        
        if (min % 1 == 0 && max % 1 == 0) {
            // If min and max are integers, return an integer
            int intResult = (int) Math.round(result);
            return Map.of("result", intResult);
        } else {
            return Map.of("result", result);
        }
    }
    
    /**
     * Calculate a mathematical expression
     */
    private Map<String, Object> calculateExpression(Map<String, Object> parameters) {
        String expression = (String) parameters.get("expression");
        
        // Sanitize and validate expression to prevent code execution
        if (!expression.matches("^[0-9+\\-*/().\\s]+$")) {
            throw new IllegalArgumentException("Invalid expression: only numbers and basic operators (+, -, *, /, (, )) are allowed");
        }
        
        try {
            // This is a simplified calculator - in production code,
            // you would use a proper expression parser
            expression = expression.replaceAll("\\s+", "");
            // For demonstration, this is extremely simplified
            double result = new javax.script.ScriptEngineManager()
                    .getEngineByName("JavaScript")
                    .eval(expression)
                    .toString()
                    .matches("^-?\\d+\\.0+$") 
                    ? Integer.parseInt(((Number)new javax.script.ScriptEngineManager()
                        .getEngineByName("JavaScript")
                        .eval(expression)).intValue() + "") 
                    : new javax.script.ScriptEngineManager()
                        .getEngineByName("JavaScript")
                        .eval(expression);
            
            return Map.of(
                "result", result,
                "expression", expression
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Error evaluating expression: " + e.getMessage());
        }
    }
    
    /**
     * Format text according to specified operation
     */
    private Map<String, Object> formatText(Map<String, Object> parameters) {
        String text = (String) parameters.get("text");
        String operation = (String) parameters.get("operation");
        
        String result = switch (operation.toLowerCase()) {
            case "upper" -> text.toUpperCase();
            case "lower" -> text.toLowerCase();
            case "capitalize" -> text.substring(0, 1).toUpperCase() + text.substring(1);
            case "trim" -> text.trim();
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
        
        return Map.of(
            "result", result,
            "original", text,
            "operation", operation
        );
    }
} 