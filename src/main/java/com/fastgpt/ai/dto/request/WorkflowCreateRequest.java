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
 * Request DTO for creating a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCreateRequest {
    
    /**
     * ID of the user creating the workflow
     */
    @NotBlank(message = "User ID cannot be empty")
    private String userId;
    
    /**
     * ID of the team if applicable
     */
    private String teamId;
    
    /**
     * Name of the workflow
     */
    @NotBlank(message = "Name cannot be empty")
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
} 