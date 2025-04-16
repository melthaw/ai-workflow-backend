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
 * 聊天节点处理器
 * 对应Next.js版本的chatNodeDispatch函数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;
    
    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.CHAT_NODE.toString();
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        try {
            log.info("Processing chat node: {}", node.getNodeId());
            
            // 获取输入参数
            String query = (String) inputs.getOrDefault("query", "");
            String systemPrompt = (String) inputs.getOrDefault("systemPrompt", "");
            Object history = inputs.getOrDefault("history", null);
            String model = (String) inputs.getOrDefault("model", "gpt-3.5-turbo");
            
            // 调用AI服务
            Map<String, Object> aiResponse = aiService.chat(query, systemPrompt, history, model);
            
            // 处理响应
            String answer = (String) aiResponse.getOrDefault("content", "");
            Map<String, Object> usage = (Map<String, Object>) aiResponse.getOrDefault("usage", new HashMap<>());
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("answerText", answer);
            
            // 构建响应数据，包含使用信息等
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("query", query);
            responseData.put("answer", answer);
            responseData.put("model", model);
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("tokens", usage);
            usages.put("model", model);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing chat node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("answerText", "Error: " + e.getMessage());
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 