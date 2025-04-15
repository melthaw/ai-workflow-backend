package com.fastgpt.ai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for workflow definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDTO {
    
    /**
     * Internal DB ID
     */
    private String id;
    
    /**
     * External workflow ID
     */
    private String workflowId;
    
    /**
     * ID of the user who owns this workflow
     */
    private String userId;
    
    /**
     * ID of the team if applicable
     */
    private String teamId;
    
    /**
     * ID of the app this workflow is associated with
     */
    private String appId;
    
    /**
     * ID of the module this workflow is part of
     */
    private String moduleId;
    
    /**
     * Name of the workflow
     */
    private String name;
    
    /**
     * Description of the workflow
     */
    private String description;
    
    /**
     * When the workflow was created
     */
    private LocalDateTime createTime;
    
    /**
     * When the workflow was last updated
     */
    private LocalDateTime updateTime;
    
    /**
     * List of nodes in the workflow
     */
    private List<NodeDefDTO> nodes;
    
    /**
     * List of edges/connections between nodes
     */
    private List<ConnectionDTO> edges;
    
    /**
     * Default input values for the workflow
     */
    private Map<String, Object> defaultInputs;
    
    /**
     * Additional workflow configuration
     */
    private Map<String, Object> config;
    
    /**
     * Published status (draft, published, etc.)
     */
    private String status;
    
    /**
     * Whether this workflow is a template
     */
    private Boolean isTemplate;
} 