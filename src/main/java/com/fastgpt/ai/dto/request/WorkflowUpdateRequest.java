package com.fastgpt.ai.dto.request;

import com.fastgpt.ai.dto.workflow.ConnectionDTO;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowUpdateRequest {
    
    /**
     * Workflow ID to update
     */
    @NotBlank(message = "Workflow ID cannot be empty")
    private String workflowId;
    
    /**
     * Name of the workflow
     */
    private String name;
    
    /**
     * Description of the workflow
     */
    private String description;
    
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
} 