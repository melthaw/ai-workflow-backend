package com.fastgpt.ai.entity.workflow;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作流节点实体
 * 对标Next.js中的RuntimeNodeItemType
 */
@Data
@NoArgsConstructor
public class Node {
    @Id
    private String id;
    
    /**
     * The unique identifier of this node in the workflow
     */
    private String nodeId;
    
    /**
     * Node name
     */
    private String name;
    
    /**
     * The type of this node
     */
    private FlowNodeTypeEnum type;
    
    /**
     * Whether this node is an entry point for the workflow
     */
    private boolean entry;
    
    /**
     * Position X coordinate
     */
    private double x;
    
    /**
     * Position Y coordinate
     */
    private double y;
    
    /**
     * Node width
     */
    private double width;
    
    /**
     * Node height
     */
    private double height;
    
    /**
     * Whether this node is selected
     */
    private boolean selected;
    
    /**
     * Node description
     */
    private String description;
    
    /**
     * Node inputs definition
     */
    private List<NodeInput> inputs = new ArrayList<>();
    
    /**
     * Node outputs definition
     */
    private List<NodeOutput> outputs = new ArrayList<>();
    
    /**
     * Whether to show status updates for this node
     */
    private boolean showStatus;
    
    /**
     * Icon for this node
     */
    private String icon;
    
    /**
     * Node color
     */
    private String color;
    
    /**
     * Whether this node is disabled
     */
    private boolean disabled;
    
    /**
     * Node configuration object
     */
    @Field("config")
    private NodeConfig config;
    
    /**
     * Node style configuration
     */
    public static class NodeConfig {
        private Map<String, Object> properties;
    }
    
    /**
     * 判断是否为入口节点
     */
    public boolean isEntry() {
        return entry;
    }
    
    /**
     * 设置入口状态
     */
    public void setEntry(boolean entry) {
        this.entry = entry;
    }
} 