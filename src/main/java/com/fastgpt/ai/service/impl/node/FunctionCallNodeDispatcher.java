package com.fastgpt.ai.service.impl.node;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 函数调用节点调度器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "functionCall";
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing function call node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            String prompt = (String) inputs.getOrDefault("prompt", "");
            
            // 获取函数定义
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> functions = inputs.containsKey("functions") && 
                inputs.get("functions") instanceof List ?
                (List<Map<String, Object>>) inputs.get("functions") : new ArrayList<>();
            
            // 选择模型
            String model = (String) inputs.getOrDefault("model", "gpt-3.5-turbo");
            
            // 调用AI服务进行函数调用
            Map<String, Object> result = aiService.functionCall(prompt, functions, model);
            
            // 获取函数调用结果
            @SuppressWarnings("unchecked")
            Map<String, Object> functionCall = (Map<String, Object>) result.getOrDefault("functionCall", new HashMap<>());
            
            String functionName = (String) functionCall.getOrDefault("name", "");
            String arguments = (String) functionCall.getOrDefault("arguments", "{}");
            
            // 解析参数
            Map<String, Object> parsedArguments = new HashMap<>();
            try {
                if (arguments != null && !arguments.isEmpty()) {
                    parsedArguments = objectMapper.readValue(arguments, Map.class);
                }
            } catch (Exception e) {
                log.warn("Failed to parse function arguments: {}", e.getMessage());
            }
            
            // 获取使用统计
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) result.getOrDefault("usage", new HashMap<>());
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("functionName", functionName);
            outputs.put("arguments", parsedArguments);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("prompt", prompt);
            responseData.put("functionName", functionName);
            responseData.put("arguments", parsedArguments);
            responseData.put("rawArguments", arguments);
            responseData.put("model", model);
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("functionCall", usage);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing function call node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("functionName", "");
            outputs.put("arguments", new HashMap<>());
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
}
