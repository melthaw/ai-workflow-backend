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
 * 文本嵌入节点调度器
 * 生成文本向量表示
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextEmbeddingNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;

    @Override
    public String getNodeType() {
        return "textEmbedding";
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing text embedding node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            String text = (String) inputs.getOrDefault("text", "");
            
            // 选择模型
            String model = (String) inputs.getOrDefault("model", "text-embedding-ada-002");
            
            // 调用AI服务生成嵌入向量
            Map<String, Object> result = aiService.embedText(text, model);
            
            // 获取嵌入向量
            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) result.getOrDefault("embedding", null);
            
            // 获取使用统计
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) result.getOrDefault("usage", new HashMap<>());
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("embedding", embedding);
            outputs.put("dimensions", embedding != null ? embedding.size() : 0);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("text", text.length() > 100 ? text.substring(0, 100) + "..." : text);
            responseData.put("model", model);
            responseData.put("dimensions", embedding != null ? embedding.size() : 0);
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("textEmbedding", usage);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing text embedding node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("embedding", null);
            outputs.put("dimensions", 0);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 