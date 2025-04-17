package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import com.fastgpt.ai.service.ToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool节点调度器
 * 对应Next.js版本的tools节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolNodeDispatcher implements NodeDispatcher {

    private final ToolService toolService;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.TOOLS.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing tool node: {}", node.getNodeId());
        
        try {
            // 获取工具ID
            String toolId = (String) inputs.getOrDefault("toolId", "");
            if (toolId == null || toolId.isEmpty()) {
                throw new IllegalArgumentException("Tool ID is required");
            }
            
            // 获取工具输入参数
            Map<String, Object> toolParams = new HashMap<>();
            if (inputs.containsKey("params") && inputs.get("params") instanceof Map) {
                toolParams = (Map<String, Object>) inputs.get("params");
            }
            
            // 调用工具服务执行工具
            Map<String, Object> toolResponse = toolService.executeTool(toolId, toolParams);
            
            // 设置返回值
            String status = "completed";
            String content = "";
            List<Map<String, Object>> steps = null;
            Map<String, Object> usage = new HashMap<>();
            
            if (toolResponse != null) {
                status = (String) toolResponse.getOrDefault("status", status);
                content = (String) toolResponse.getOrDefault("content", "");
                
                if (toolResponse.containsKey("steps") && toolResponse.get("steps") instanceof List) {
                    steps = (List<Map<String, Object>>) toolResponse.get("steps");
                }
                
                if (toolResponse.containsKey("usage") && toolResponse.get("usage") instanceof Map) {
                    usage = (Map<String, Object>) toolResponse.get("usage");
                }
            }
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("status", status);
            outputs.put("content", content);
            outputs.put("toolId", toolId);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("toolId", toolId);
            responseData.put("status", status);
            responseData.put("content", content);
            responseData.put("steps", steps);
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("tool", usage);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing tool node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("status", "error");
            outputs.put("content", "Error: " + e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 