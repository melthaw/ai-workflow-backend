package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for node input/output definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeIODTO {
    
    /**
     * Unique key for this input/output
     */
    private String key;
    
    /**
     * Human-readable name
     */
    private String name;
    
    /**
     * Data type (e.g., string, number, boolean, object)
     */
    private String type;
    
    /**
     * Description of this input/output
     */
    private String description;
    
    /**
     * Whether this input is required
     */
    private Boolean required;
    
    /**
     * Whether this input is connected to another node
     */
    private Boolean connected;
    
    /**
     * ID of the source node (for inputs)
     */
    private String connectionSourceId;
    
    /**
     * Output key of the source node (for inputs)
     */
    private String connectionOutputKey;
    
    /**
     * Default value if not connected or provided
     */
    private Object defaultValue;
    
    /**
     * Additional configuration options
     */
    private Object options;
} 