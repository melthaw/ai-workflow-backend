package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模态处理节点调度器
 * 处理图像、音频等多模态输入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalProcessingNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;

    @Override
    public String getNodeType() {
        return "multimodalProcessing";
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing multimodal node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            String prompt = (String) inputs.getOrDefault("prompt", "");
            
            // 获取多模态输入（例如图像URL列表）
            @SuppressWarnings("unchecked")
            List<String> imageUrls = inputs.containsKey("imageUrls") && inputs.get("imageUrls") instanceof List ?
                (List<String>) inputs.get("imageUrls") : new ArrayList<>();
            
            // 创建多模态输入参数
            Map<String, Object> multimodalInput = new HashMap<>();
            multimodalInput.put("prompt", prompt);
            multimodalInput.put("imageUrls", imageUrls);
            
            // 选择模型
            String model = (String) inputs.getOrDefault("model", "gpt-4-vision");
            
            // 调用AI服务处理多模态输入
            Map<String, Object> result = processMultimodal(multimodalInput, model);
            
            // 获取处理结果
            String response = (String) result.getOrDefault("content", "");
            
            // 获取使用统计
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) result.getOrDefault("usage", new HashMap<>());
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("response", response);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("prompt", prompt);
            responseData.put("imageCount", imageUrls.size());
            responseData.put("response", response);
            responseData.put("model", model);
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("multimodal", usage);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing multimodal node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("response", "Error: " + e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 处理多模态输入
     * 实际项目中，应该调用支持多模态的AI服务来完成
     */
    private Map<String, Object> processMultimodal(Map<String, Object> multimodalInput, String model) {
        // 这里只是简单模拟
        // 实际应该调用AiService中的多模态处理方法
        
        String prompt = (String) multimodalInput.getOrDefault("prompt", "");
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) multimodalInput.getOrDefault("imageUrls", new ArrayList<>());
        
        StringBuilder response = new StringBuilder();
        response.append("分析结果:\n\n");
        
        if (!imageUrls.isEmpty()) {
            response.append("检测到 ").append(imageUrls.size()).append(" 张图像。\n\n");
            for (int i = 0; i < imageUrls.size(); i++) {
                response.append("图像 ").append(i + 1).append(": 这是一张普通图片\n");
            }
        }
        
        if (!prompt.isEmpty()) {
            response.append("\n用户提问: ").append(prompt).append("\n");
            response.append("回答: 这是对图像内容的简单描述和对问题的回答。");
        }
        
        // 使用统计
        Map<String, Object> usage = new HashMap<>();
        usage.put("prompt_tokens", prompt.length() / 4 + imageUrls.size() * 1000);  // 图像通常消耗更多token
        usage.put("completion_tokens", 100);
        usage.put("total_tokens", prompt.length() / 4 + imageUrls.size() * 1000 + 100);
        
        // 结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", response.toString());
        result.put("usage", usage);
        
        return result;
    }
} 