package com.fastgpt.ai.dto.workflow;

import com.fastgpt.ai.entity.workflow.Edge;
import com.fastgpt.ai.entity.workflow.Node;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 工作流DTO
 * 对标Next.js版本中的工作流结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDTO {
    /**
     * 工作流ID
     */
    private String workflowId;
    
    /**
     * 工作流名称
     */
    private String name;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 团队ID
     */
    private String teamId;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 工作流节点列表
     */
    private List<Node> nodes = new ArrayList<>();
    
    /**
     * 工作流边列表
     */
    private List<Edge> edges = new ArrayList<>();
} 