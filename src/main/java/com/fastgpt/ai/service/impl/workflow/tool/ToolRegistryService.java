package com.fastgpt.ai.service.impl.workflow.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Service for registering and managing tools that can be called by AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolRegistryService {

    // Map of tool name to tool executor function
    private final Map<String, ToolExecutor> toolRegistry = new ConcurrentHashMap<>();
    
    // Map of tool name to tool definition (for AI function calling)
    private final Map<String, Map<String, Object>> toolDefinitions = new ConcurrentHashMap<>();
    
    // Standard tools to register automatically
    private final List<ToolProvider> toolProviders;
    
    /**
     * Initialize the registry with standard tools
     */
    @PostConstruct
    public void init() {
        log.info("Initializing tool registry");
        
        // Register all tools from providers
        for (ToolProvider provider : toolProviders) {
            try {
                provider.registerTools(this);
                log.info("Registered tools from provider: {}", provider.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Failed to register tools from provider {}: {}", 
                        provider.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        
        log.info("Tool registry initialized with {} tools", toolRegistry.size());
    }
    
    /**
     * Register a new tool
     * 
     * @param name Tool name
     * @param description Tool description
     * @param parameterSchema JSON Schema definition for parameters
     * @param executor Function to execute the tool
     */
    public void registerTool(String name, String description, Map<String, Object> parameterSchema, 
            Function<Map<String, Object>, Object> executor) {
        
        // Create tool definition for AI function calling
        Map<String, Object> toolDef = new HashMap<>();
        toolDef.put("name", name);
        toolDef.put("description", description);
        toolDef.put("parameters", parameterSchema);
        
        // Create tool executor
        ToolExecutor toolExecutor = new ToolExecutor(name, description, parameterSchema, executor);
        
        // Register tool
        toolRegistry.put(name, toolExecutor);
        toolDefinitions.put(name, toolDef);
        
        log.info("Registered tool: {}", name);
    }
    
    /**
     * Check if a tool is registered
     * 
     * @param name Tool name
     * @return True if the tool is registered
     */
    public boolean hasTool(String name) {
        return toolRegistry.containsKey(name);
    }
    
    /**
     * Get a tool executor by name
     * 
     * @param name Tool name
     * @return Tool executor or null if not found
     */
    public ToolExecutor getTool(String name) {
        return toolRegistry.get(name);
    }
    
    /**
     * Get all tool definitions (for AI function calling)
     * 
     * @return List of tool definitions
     */
    public List<Map<String, Object>> getAllToolDefinitions() {
        return new ArrayList<>(toolDefinitions.values());
    }
    
    /**
     * Get tool definitions for a specific category
     * 
     * @param category Tool category
     * @return List of tool definitions in the category
     */
    public List<Map<String, Object>> getToolDefinitionsByCategory(String category) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : toolDefinitions.entrySet()) {
            Map<String, Object> toolDef = entry.getValue();
            if (toolDef.containsKey("category") && category.equals(toolDef.get("category"))) {
                result.add(toolDef);
            }
        }
        
        return result;
    }
    
    /**
     * Execute a tool by name with parameters
     * 
     * @param name Tool name
     * @param parameters Tool parameters
     * @return Tool execution result
     * @throws IllegalArgumentException if tool not found
     */
    public Object executeTool(String name, Map<String, Object> parameters) {
        ToolExecutor executor = toolRegistry.get(name);
        
        if (executor == null) {
            throw new IllegalArgumentException("Tool not found: " + name);
        }
        
        try {
            log.info("Executing tool: {} with parameters: {}", name, parameters);
            Object result = executor.execute(parameters);
            log.debug("Tool execution result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error executing tool {}: {}", name, e.getMessage(), e);
            throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Class representing a tool executor with metadata
     */
    public static class ToolExecutor {
        private final String name;
        private final String description;
        private final Map<String, Object> parameterSchema;
        private final Function<Map<String, Object>, Object> executor;
        
        public ToolExecutor(String name, String description, Map<String, Object> parameterSchema,
                Function<Map<String, Object>, Object> executor) {
            this.name = name;
            this.description = description;
            this.parameterSchema = Collections.unmodifiableMap(new HashMap<>(parameterSchema));
            this.executor = executor;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Map<String, Object> getParameterSchema() {
            return parameterSchema;
        }
        
        public Object execute(Map<String, Object> parameters) {
            return executor.apply(parameters);
        }
    }
} 