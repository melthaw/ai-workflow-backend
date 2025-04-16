package com.fastgpt.ai.service.impl.workflow.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.NodeDispatcher;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Node dispatcher for AI function/tool calling
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionCallNodeDispatcher implements NodeDispatcher {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    
    private static final String NODE_TYPE = "ai.function.call";
    
    // Default parameters
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final String DEFAULT_SYSTEM_PROMPT = 
            "You are an AI assistant capable of calling functions to help users. " +
            "Call the appropriate function based on the user's request.";
    
    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            // Convert node to DTO for easier handling
            NodeDefDTO nodeDefDTO = convertToNodeDefDTO(node);
            return processFunctionCall(nodeDefDTO, inputs);
        } catch (Exception e) {
            log.error("Error in function call node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Function call failed: " + e.getMessage());
        }
    }
    
    /**
     * Process function call based on inputs
     */
    private NodeOutDTO processFunctionCall(NodeDefDTO node, Map<String, Object> inputs) {
        log.info("Processing function call node: {}", node.getId());
        
        try {
            // Extract node configuration
            Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
            
            // Get configuration parameters
            String model = getStringParam(nodeData, "model", DEFAULT_MODEL);
            String systemPrompt = getStringParam(nodeData, "systemPrompt", DEFAULT_SYSTEM_PROMPT);
            
            // Get functions from config
            List<Map<String, Object>> functions = new ArrayList<>();
            if (nodeData.containsKey("functions") && nodeData.get("functions") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> configFunctions = (List<Map<String, Object>>) nodeData.get("functions");
                functions = configFunctions;
            } else if (inputs.containsKey("functions") && inputs.get("functions") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> inputFunctions = (List<Map<String, Object>>) inputs.get("functions");
                functions = inputFunctions;
            }
            
            if (functions.isEmpty()) {
                return NodeOutDTO.error("No functions provided for function call node");
            }
            
            // Validate function schemas
            for (Map<String, Object> function : functions) {
                if (function.containsKey("parameters") && function.get("parameters") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
                    
                    // Make sure schema is valid
                    try {
                        validateJsonSchema(objectMapper.writeValueAsString(parameters));
                    } catch (Exception e) {
                        log.warn("Invalid JSON Schema for function {}: {}", 
                                function.getOrDefault("name", "unknown"), e.getMessage());
                    }
                }
            }
            
            // Get user message
            String userMessage = getStringParam(inputs, "message", "");
            if (userMessage.isEmpty()) {
                return NodeOutDTO.error("No user message provided for function call");
            }
            
            // Create system message
            SystemMessage systemMessageObj = new SystemMessage(systemPrompt);
            
            // Create user message
            UserMessage userMessageObj = new UserMessage(userMessage);
            
            // Create prompt with messages
            List<Message> messages = new ArrayList<>();
            messages.add(systemMessageObj);
            messages.add(userMessageObj);
            
            // Set model options and tools (using tools format for compatibility with Spring AI 0.8.1)
            Map<String, Object> options = new HashMap<>();
            options.put("model", model);
            options.put("temperature", 0.5);
            
            // Convert functions to tools format
            List<Map<String, Object>> tools = new ArrayList<>();
            for (Map<String, Object> function : functions) {
                Map<String, Object> tool = new HashMap<>();
                tool.put("type", "function");
                tool.put("function", function);
                tools.add(tool);
            }
            options.put("tools", tools);
            
            // Call AI service for function calling
            // Use compatible Prompt constructor format for Spring AI 0.8.1
            Prompt prompt = new Prompt(messages);
            log.debug("Sending function call prompt: {}", prompt);
            
            ChatResponse response = chatClient.call(prompt);
            Generation generation = response.getResults().get(0);
            String result = generation.getOutput().getContent();
            log.debug("Received function call response: {}", result);
            
            // Extract function call info from response
            Map<String, Object> functionCallInfo = new HashMap<>();
            try {
                // In Spring AI 0.8.1, function call extraction needs different approach
                functionCallInfo = extractFunctionCallInfo(generation);
            } catch (Exception e) {
                log.error("Error extracting function call info: {}", e.getMessage());
                return NodeOutDTO.error("Failed to extract function call info: " + e.getMessage());
            }
            
            // Perform JSON Schema validation on function arguments if schema exists
            if (functionCallInfo.containsKey("name") && functionCallInfo.containsKey("arguments")) {
                String functionName = (String) functionCallInfo.get("name");
                Map<String, Object> args = (Map<String, Object>) functionCallInfo.get("arguments");
                
                // Find the function definition
                for (Map<String, Object> function : functions) {
                    if (functionName.equals(function.get("name"))) {
                        if (function.containsKey("parameters") && function.get("parameters") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
                            
                            try {
                                // Validate arguments against schema
                                Set<ValidationMessage> validationResult = validateArguments(
                                        objectMapper.writeValueAsString(parameters), 
                                        objectMapper.writeValueAsString(args));
                                
                                if (!validationResult.isEmpty()) {
                                    String validationErrors = validationResult.stream()
                                            .map(ValidationMessage::getMessage)
                                            .collect(Collectors.joining(", "));
                                    
                                    log.warn("Function arguments validation failed: {}", validationErrors);
                                    functionCallInfo.put("validationErrors", validationErrors);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to validate function arguments: {}", e.getMessage());
                            }
                        }
                        break;
                    }
                }
            }
            
            // Prepare output
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("functionName", functionCallInfo.get("name"));
            outputs.put("functionArgs", functionCallInfo.get("arguments"));
            outputs.put("response", result);
            outputs.put("functions", functions);
            
            if (functionCallInfo.containsKey("validationErrors")) {
                outputs.put("validationErrors", functionCallInfo.get("validationErrors"));
            }
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("model", model);
            metadata.put("functionCount", functions.size());
            
            return NodeOutDTO.success(outputs, metadata);
            
        } catch (Exception e) {
            log.error("Error in function call processing: {}", e.getMessage(), e);
            throw new WorkflowExecutionException("Function call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract function call information from the Generation
     * Adapted for Spring AI 0.8.1
     */
    private Map<String, Object> extractFunctionCallInfo(Generation generation) {
        Map<String, Object> functionCallInfo = new HashMap<>();
        
        try {
            // Spring AI 0.8.1 stores function call info in the generation metadata
            // This is a simplification - exact path may vary by Spring AI version
            Map<String, Object> metadata = generation.getMetadata();
            
            if (metadata != null && metadata.containsKey("tool_calls")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) metadata.get("tool_calls");
                
                if (!toolCalls.isEmpty()) {
                    Map<String, Object> toolCall = toolCalls.get(0);
                    
                    if (toolCall.containsKey("function")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                        
                        String name = (String) function.getOrDefault("name", "unknown");
                        functionCallInfo.put("name", name);
                        
                        // Parse arguments string if present
                        if (function.containsKey("arguments")) {
                            String argumentsStr = (String) function.get("arguments");
                            try {
                                Map<String, Object> args = objectMapper.readValue(argumentsStr, LinkedHashMap.class);
                                functionCallInfo.put("arguments", args);
                            } catch (Exception e) {
                                // If JSON parsing fails, just use the raw string
                                functionCallInfo.put("arguments", argumentsStr);
                            }
                        } else {
                            functionCallInfo.put("arguments", new HashMap<>());
                        }
                    }
                }
            } else {
                // Fallback to parsing content for older Spring AI versions
                String content = generation.getOutput().getContent();
                functionCallInfo = parseFunctionCallFromContent(content);
            }
        } catch (Exception e) {
            log.error("Failed to extract function call info: {}", e.getMessage());
            functionCallInfo.put("name", "error");
            functionCallInfo.put("arguments", "{}");
        }
        
        return functionCallInfo;
    }
    
    /**
     * Parse function call information from text content
     * Fallback method if metadata extraction fails
     */
    private Map<String, Object> parseFunctionCallFromContent(String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "unknown");
        result.put("arguments", new HashMap<>());
        
        try {
            // Try to parse function call from JSON format
            if (content.contains("\"function\"") || content.contains("\"name\"")) {
                // Extract function name
                int nameStart = content.indexOf("\"name\"");
                if (nameStart != -1) {
                    int valueStart = content.indexOf(":", nameStart) + 1;
                    int valueEnd = content.indexOf(",", valueStart);
                    if (valueEnd == -1) {
                        valueEnd = content.indexOf("}", valueStart);
                    }
                    
                    if (valueEnd > valueStart) {
                        String nameRaw = content.substring(valueStart, valueEnd).trim();
                        // Remove quotes if present
                        String name = nameRaw.replaceAll("^\"|\"$|^'|'$", "");
                        result.put("name", name);
                    }
                }
                
                // Extract arguments
                int argsStart = content.indexOf("\"arguments\"");
                if (argsStart != -1) {
                    int valueStart = content.indexOf(":", argsStart) + 1;
                    // Find the start of JSON object
                    int jsonStart = content.indexOf("{", valueStart);
                    if (jsonStart != -1) {
                        // Find matching closing brace
                        int depth = 1;
                        int jsonEnd = -1;
                        for (int i = jsonStart + 1; i < content.length(); i++) {
                            char c = content.charAt(i);
                            if (c == '{') depth++;
                            else if (c == '}') depth--;
                            
                            if (depth == 0) {
                                jsonEnd = i + 1;
                                break;
                            }
                        }
                        
                        if (jsonEnd > jsonStart) {
                            String argsJson = content.substring(jsonStart, jsonEnd);
                            try {
                                Map<String, Object> args = objectMapper.readValue(argsJson, LinkedHashMap.class);
                                result.put("arguments", args);
                            } catch (Exception e) {
                                log.warn("Failed to parse arguments JSON: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing function call from content: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate a JSON Schema to ensure it's valid
     */
    private void validateJsonSchema(String schemaJson) throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        factory.getSchema(schemaJson);
    }
    
    /**
     * Validate arguments against a JSON Schema
     */
    private Set<ValidationMessage> validateArguments(String schemaJson, String argumentsJson) throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode argumentsNode = objectMapper.readTree(argumentsJson);
        return schema.validate(argumentsNode);
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