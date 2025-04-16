package com.fastgpt.ai.dto.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a function definition for AI function calling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDefinition {
    
    /**
     * Function name
     */
    private String name;
    
    /**
     * Function description
     */
    private String description;
    
    /**
     * Map of parameter definitions
     * Each parameter has properties like type, description, etc.
     */
    private Map<String, Object> parameters;
    
    /**
     * Whether this function is required to be called
     */
    private Boolean required;
    
    /**
     * Builder method to create a simple parameter
     * 
     * @param name Parameter name
     * @param type Parameter type (string, number, boolean, etc.)
     * @param description Parameter description
     * @param required Whether parameter is required
     * @return Parameter definition
     */
    public static Map<String, Object> createParameter(String name, String type, String description, boolean required) {
        return Map.of(
            "type", type,
            "description", description,
            "required", required
        );
    }
    
    /**
     * Builder method to create an enum parameter
     * 
     * @param name Parameter name
     * @param description Parameter description
     * @param options Enum options
     * @param required Whether parameter is required
     * @return Parameter definition
     */
    public static Map<String, Object> createEnumParameter(String name, String description, List<String> options, boolean required) {
        return Map.of(
            "type", "string",
            "description", description,
            "enum", options,
            "required", required
        );
    }
    
    /**
     * Converts to OpenAI function definition format
     * 
     * @return Map representing OpenAI function definition
     */
    public Map<String, Object> toOpenAIFormat() {
        Map<String, Object> functionDef = Map.of(
            "name", this.name,
            "description", this.description,
            "parameters", Map.of(
                "type", "object",
                "properties", this.parameters,
                "required", getRequiredParameters()
            )
        );
        
        return functionDef;
    }
    
    /**
     * Get list of required parameter names
     * 
     * @return List of required parameter names
     */
    private List<String> getRequiredParameters() {
        return parameters.entrySet().stream()
            .filter(entry -> entry.getValue() instanceof Map && 
                    ((Map<?,?>)entry.getValue()).containsKey("required") && 
                    Boolean.TRUE.equals(((Map<?,?>)entry.getValue()).get("required")))
            .map(Map.Entry::getKey)
            .toList();
    }
} 