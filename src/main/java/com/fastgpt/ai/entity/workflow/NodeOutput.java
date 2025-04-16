package com.fastgpt.ai.entity.workflow;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

/**
 * 节点输出定义
 * 对标Next.js中的节点输出参数
 */
@Data
@NoArgsConstructor
public class NodeOutput {
    /**
     * 输出键名
     */
    private String key;
    
    /**
     * 输出标签
     */
    private String label;
    
    /**
     * 输出类型
     */
    private String type;
    
    /**
     * 输出值类型
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
     * 输出值
     */
    private Object value;
    
    /**
     * 额外属性
     */
    private Map<String, Object> properties;
} 