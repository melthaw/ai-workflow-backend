package com.fastgpt.ai.entity.workflow;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 节点输入定义
 * 对标Next.js中的节点输入参数
 */
@Data
@NoArgsConstructor
public class NodeInput {
    /**
     * 输入键名
     */
    private String key;
    
    /**
     * 输入标签
     */
    private String label;
    
    /**
     * 输入类型
     */
    private String type;
    
    /**
     * 输入值
     */
    private Object value;
    
    /**
     * 输入值类型
     */
    private String valueType;
    
    /**
     * 是否必填
     */
    private boolean required;
    
    /**
     * 默认值
     */
    @Field("default_value")
    private Object defaultValue;
    
    /**
     * 是否可编辑
     */
    private boolean canEdit;
    
    /**
     * 渲染类型列表
     */
    private List<String> renderTypeList = new ArrayList<>();
    
    /**
     * 获取渲染类型
     */
    public String getRenderType() {
        return renderTypeList != null && !renderTypeList.isEmpty() ? 
            renderTypeList.get(0) : null;
    }
    
    /**
     * 额外属性
     */
    private Map<String, Object> properties;
} 