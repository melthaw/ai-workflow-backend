package com.fastgpt.ai.entity.workflow;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

/**
 * 工作流连线实体
 * 对标Next.js中的RuntimeEdgeItemType
 */
@Data
@NoArgsConstructor
public class Edge {
    /**
     * 唯一标识符
     */
    @Id
    private String id;
    
    /**
     * 边的唯一ID
     */
    @Field("edge_id")
    private String edgeId;
    
    /**
     * 源节点ID
     */
    @Field("source")
    private String sourceNodeId;
    
    /**
     * 源节点连接点
     */
    @Field("source_handle")
    private String sourceHandle;
    
    /**
     * 目标节点ID
     */
    @Field("target")
    private String targetNodeId;
    
    /**
     * 目标节点连接点
     */
    @Field("target_handle")
    private String targetHandle;
    
    /**
     * 边的标签
     */
    private String label;
    
    /**
     * 边的状态：waiting, active, skipped
     */
    private String status;
    
    /**
     * 获取源节点ID (兼容方法)
     */
    public String getSource() {
        return sourceNodeId;
    }
    
    /**
     * 设置源节点ID (兼容方法)
     */
    public void setSource(String source) {
        this.sourceNodeId = source;
    }
    
    /**
     * 获取目标节点ID (兼容方法)
     */
    public String getTarget() {
        return targetNodeId;
    }
    
    /**
     * 设置目标节点ID (兼容方法)
     */
    public void setTarget(String target) {
        this.targetNodeId = target;
    }
    
    /**
     * 设置边的状态
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Optional condition that must be met for this edge to be traversed
     */
    private String condition;
    
    /**
     * Additional edge properties and metadata
     */
    private Map<String, Object> data;
} 