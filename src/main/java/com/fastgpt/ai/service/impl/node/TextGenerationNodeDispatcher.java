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
import java.util.Map;

/**
 * 文本生成节点调度器
 * 使用AI模型生成文本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextGenerationNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;

    @Override
    public String getNodeType() {
        return "textGeneration";
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing text generation node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            String prompt = (String) inputs.getOrDefault("prompt", "");
            
            // 获取生成选项
            String model = (String) inputs.getOrDefault("model", "gpt-3.5-turbo");
            
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", inputs.getOrDefault("temperature", 0.7));
            options.put("maxTokens", inputs.getOrDefault("maxTokens", 500));
            options.put("topP", inputs.getOrDefault("topP", 1.0));
            options.put("frequencyPenalty", inputs.getOrDefault("frequencyPenalty", 0.0));
            options.put("presencePenalty", inputs.getOrDefault("presencePenalty", 0.0));
            
            // 调用AI服务进行文本生成
            Map<String, Object> result = aiService.generateText(prompt, model, options);
            
            // 获取生成的文本
            String generatedText = (String) result.getOrDefault("text", "");
            
            // 获取使用统计
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) result.getOrDefault("usage", new HashMap<>());
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("text", generatedText);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("prompt", prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);
            responseData.put("model", model);
            responseData.put("text", generatedText);
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("textGeneration", usage);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing text generation node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("text", "");
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 