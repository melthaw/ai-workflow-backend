package com.fastgpt.ai.entity.workflow;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

/**
 * Entity representing an edge (connection) between workflow nodes
 */
@Data
public class Edge {
    @Id
    private String id;
    
    /**
     * Edge identifier within the workflow
     */
    @Field("edge_id")
    private String edgeId;
    
    /**
     * Source node ID
     */
    @Field("source")
    private String sourceNodeId;
    
    /**
     * Source node output identifier
     */
    @Field("source_handle")
    private String sourceHandle;
    
    /**
     * Target node ID
     */
    @Field("target")
    private String targetNodeId;
    
    /**
     * Target node input identifier
     */
    @Field("target_handle")
    private String targetHandle;
    
    /**
     * Optional condition that must be met for this edge to be traversed
     */
    private String condition;
    
    /**
     * Additional edge properties and metadata
     */
    private Map<String, Object> data;
} 