package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话管理节点调度器
 * 管理对话历史记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationManagerNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return "conversationManager";
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing conversation manager node: {}", node.getNodeId());
        
        try {
            // 获取当前查询
            String query = (String) inputs.getOrDefault("query", "");
            
            // 获取对话历史
            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = inputs.containsKey("history") && inputs.get("history") instanceof List ?
                (List<Map<String, String>>) inputs.get("history") : new ArrayList<>();
            
            // 获取最新响应
            String response = (String) inputs.getOrDefault("response", "");
            
            // 获取管理选项
            int maxHistoryLength = inputs.containsKey("maxHistoryLength") ? 
                Integer.parseInt(String.valueOf(inputs.get("maxHistoryLength"))) : 10;
            boolean includeCurrentExchange = (boolean) inputs.getOrDefault("includeCurrentExchange", true);
            
            // 处理对话历史
            List<Map<String, String>> updatedHistory = processHistory(history, query, response, maxHistoryLength, includeCurrentExchange);
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("history", updatedHistory);
            outputs.put("messageCount", updatedHistory.size());
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("query", query);
            responseData.put("response", response);
            responseData.put("historyLength", updatedHistory.size());
            responseData.put("maxHistoryLength", maxHistoryLength);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing conversation manager node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("history", new ArrayList<>());
            outputs.put("messageCount", 0);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 处理对话历史
     */
    private List<Map<String, String>> processHistory(
            List<Map<String, String>> history, 
            String query, 
            String response, 
            int maxHistoryLength,
            boolean includeCurrentExchange) {
        
        // 创建新的历史记录列表
        List<Map<String, String>> updatedHistory = new ArrayList<>(history);
        
        // 如果需要包含当前对话，则添加到历史记录中
        if (includeCurrentExchange) {
            // 添加用户消息
            if (query != null && !query.isEmpty()) {
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", query);
                updatedHistory.add(userMessage);
            }
            
            // 添加助手消息
            if (response != null && !response.isEmpty()) {
                Map<String, String> assistantMessage = new HashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", response);
                updatedHistory.add(assistantMessage);
            }
        }
        
        // 如果历史记录超过最大长度，则裁剪
        int startIndex = Math.max(0, updatedHistory.size() - maxHistoryLength);
        if (startIndex > 0) {
            updatedHistory = updatedHistory.subList(startIndex, updatedHistory.size());
        }
        
        return updatedHistory;
    }
} 