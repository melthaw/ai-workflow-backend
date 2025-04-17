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

/**
 * 文本分类节点调度器
 * 用于对文本进行分类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextClassificationNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;

    @Override
    public String getNodeType() {
        return "textClassification";
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing text classification node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            String text = (String) inputs.getOrDefault("text", "");
            
            // 获取分类类别
            @SuppressWarnings("unchecked")
            List<String> categories = inputs.containsKey("categories") && inputs.get("categories") instanceof List ?
                (List<String>) inputs.get("categories") : null;
            
            // 获取自定义提示词
            String customPrompt = (String) inputs.getOrDefault("customPrompt", "");
            
            // 调用AI服务进行分类
            Map<String, Object> result = classifyText(text, categories, customPrompt);
            
            // 获取分类结果
            @SuppressWarnings("unchecked")
            Map<String, Object> classificationResult = (Map<String, Object>) result.getOrDefault("classification", new HashMap<>());
            String category = (String) classificationResult.getOrDefault("category", "");
            double confidence = (double) classificationResult.getOrDefault("confidence", 0.0);
            
            // 获取使用统计
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) result.getOrDefault("usage", new HashMap<>());
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("category", category);
            outputs.put("confidence", confidence);
            outputs.put("classificationResult", classificationResult);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("text", text);
            responseData.put("categories", categories);
            responseData.put("category", category);
            responseData.put("confidence", confidence);
            responseData.put("fullResult", classificationResult);
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("textClassification", usage);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing text classification node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("category", "");
            outputs.put("confidence", 0.0);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 执行文本分类
     * 实际项目中，应该调用AI服务来完成
     */
    private Map<String, Object> classifyText(String text, List<String> categories, String customPrompt) {
        // 这里只是简单模拟
        // 实际应该调用AiService
        
        Map<String, Object> classification = new HashMap<>();
        
        // 简单模拟分类逻辑
        if (categories != null && !categories.isEmpty()) {
            String category = categories.get(0);
            if (text.toLowerCase().contains(category.toLowerCase())) {
                classification.put("category", category);
                classification.put("confidence", 0.9);
            } else {
                classification.put("category", categories.get(categories.size() > 1 ? 1 : 0));
                classification.put("confidence", 0.6);
            }
        } else {
            // 默认分类
            classification.put("category", "其他");
            classification.put("confidence", 0.5);
        }
        
        // 使用统计
        Map<String, Object> usage = new HashMap<>();
        usage.put("tokens", text.length() / 4);  // 简单估算token数
        
        // 结果
        Map<String, Object> result = new HashMap<>();
        result.put("classification", classification);
        result.put("usage", usage);
        
        return result;
    }
} 