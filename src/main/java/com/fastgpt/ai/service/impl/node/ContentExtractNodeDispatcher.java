package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 内容提取节点调度器
 * 对应Next.js版本的contentExtract节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentExtractNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.CONTENT_EXTRACT.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing content extract node: {}", node.getNodeId());
        
        try {
            // 获取输入内容和提取字段配置
            String content = getStringValue(inputs, "content", "");
            List<Map<String, Object>> extractKeys = getListValue(inputs, "extractKeys", new ArrayList<>());
            
            if (content.isEmpty()) {
                return NodeOutDTO.error("没有要提取的内容");
            }
            
            if (extractKeys.isEmpty()) {
                return NodeOutDTO.error("未指定要提取的字段");
            }
            
            // 构建提示词
            StringBuilder systemPrompt = new StringBuilder("请从以下内容中提取信息：\n\n");
            
            systemPrompt.append("需要提取的字段：\n");
            for (Map<String, Object> field : extractKeys) {
                String key = getStringValue(field, "key", "");
                String description = getStringValue(field, "description", "");
                if (!key.isEmpty()) {
                    systemPrompt.append("- ").append(key);
                    if (!description.isEmpty()) {
                        systemPrompt.append("（").append(description).append("）");
                    }
                    systemPrompt.append("\n");
                }
            }
            
            systemPrompt.append("\n内容：\n").append(content);
            systemPrompt.append("\n\n请以JSON格式返回提取结果，只返回JSON，不要有其他内容。");
            
            // 调用AI服务进行提取
            String aiResponse = aiService.generateSimpleResponse(systemPrompt.toString(), null);
            
            // 解析提取的字段
            Map<String, Object> extractedFields = parseJsonResponse(aiResponse);
            
            // 创建输出
            return NodeOutDTO.success(extractedFields);
            
        } catch (Exception e) {
            log.error("Error in content extract node: {}", e.getMessage(), e);
            
            return NodeOutDTO.error("内容提取失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析JSON响应
     */
    private Map<String, Object> parseJsonResponse(String jsonString) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 移除可能的非JSON前缀和后缀
            String cleanJson = jsonString.trim();
            
            // 找到JSON的开始和结束位置
            int start = cleanJson.indexOf('{');
            int end = cleanJson.lastIndexOf('}');
            
            if (start >= 0 && end > start) {
                cleanJson = cleanJson.substring(start, end + 1);
                
                // 使用Jackson或其他JSON库解析
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                result = mapper.readValue(cleanJson, Map.class);
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response: {}", e.getMessage());
            // 返回空结果，避免处理失败
        }
        
        return result;
    }
    
    /**
     * 获取字符串值，带默认值
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof String) {
                return (String) value;
            }
            return String.valueOf(value);
        }
        return defaultValue;
    }
    
    /**
     * 获取列表值，带默认值
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getListValue(Map<String, Object> map, String key, List<T> defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) instanceof List) {
            return (List<T>) map.get(key);
        }
        return defaultValue;
    }
} 