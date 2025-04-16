package com.fastgpt.ai.service.impl.workflow.tool;

/**
 * Interface for tool providers that register tools with the tool registry
 */
public interface ToolProvider {
    
    /**
     * Register tools with the tool registry
     * 
     * @param registry The tool registry to register tools with
     */
    void registerTools(ToolRegistryService registry);
    
    /**
     * Get the category of tools provided by this provider
     * 
     * @return Tool category
     */
    default String getCategory() {
        return "general";
    }
} 