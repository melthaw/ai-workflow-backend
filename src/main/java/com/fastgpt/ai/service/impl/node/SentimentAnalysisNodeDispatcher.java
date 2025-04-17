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
 * 情感分析节点调度器
 * 分析文本情感色彩
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SentimentAnalysisNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;

    @Override
    public String getNodeType() {
        return "sentimentAnalysis";
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing sentiment analysis node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            String text = (String) inputs.getOrDefault("text", "");
            
            // 获取分析选项
            Map<String, Object> options = new HashMap<>();
            options.put("languages", inputs.getOrDefault("languages", "zh,en"));
            options.put("includeDetails", inputs.getOrDefault("includeDetails", true));
            
            // 调用AI服务进行情感分析
            Map<String, Object> result = aiService.analyzeSentiment(text, options);
            
            // 获取情感分析结果
            @SuppressWarnings("unchecked")
            Map<String, Object> sentimentResult = (Map<String, Object>) result.getOrDefault("sentiment", new HashMap<>());
            
            String sentiment = (String) sentimentResult.getOrDefault("sentiment", "neutral");
            double score = (double) sentimentResult.getOrDefault("score", 0.5);
            
            // 获取详细分析（如有）
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) sentimentResult.getOrDefault("details", new HashMap<>());
            
            // 获取使用统计
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) result.getOrDefault("usage", new HashMap<>());
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("sentiment", sentiment);
            outputs.put("score", score);
            outputs.put("details", details);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("text", text.length() > 100 ? text.substring(0, 100) + "..." : text);
            responseData.put("sentiment", sentiment);
            responseData.put("score", score);
            responseData.put("details", details);
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("sentimentAnalysis", usage);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing sentiment analysis node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("sentiment", "neutral");
            outputs.put("score", 0.5);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 