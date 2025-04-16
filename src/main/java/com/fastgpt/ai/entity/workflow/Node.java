package com.fastgpt.ai.entity.workflow;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

/**
 * Base class for workflow nodes
 */
@Data
public class Node {
    @Id
    private String id;
    
    /**
     * Node identifier within the workflow
     */
    @Field("node_id") 
    private String nodeId;
    
    /**
     * Node type from FlowNodeTypeEnum
     */
    private String type;
    
    /**
     * Display name of the node
     */
    private String name;
    
    /**
     * X position in the workflow UI
     */
    @Field("pos_x")
    private Integer posX;
    
    /**
     * Y position in the workflow UI
     */
    @Field("pos_y")
    private Integer posY;
    
    /**
     * Node inputs definition
     */
    private List<NodeIO> inputs;
    
    /**
     * Node outputs definition
     */
    private List<NodeIO> outputs;
    
    /**
     * Node configuration and parameters
     */
    private Map<String, Object> data;
    
    /**
     * Class representing an input or output for a node
     */
    @Data
    public static class NodeIO {
        /**
         * Input/output identifier
         */
        private String key;
        
        /**
         * Display label
         */
        private String label;
        
        /**
         * Data type (string, number, boolean, array, object)
         */
        private String type;
        
        /**
         * Whether this input/output is required
         */
        private Boolean required;
        
        /**
         * Default value if any
         */
        @Field("default_value")
        private Object defaultValue;
        
        /**
         * Additional properties for this input/output
         */
        private Map<String, Object> properties;
    }
} 