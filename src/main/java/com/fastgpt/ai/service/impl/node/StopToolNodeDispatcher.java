package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 停止工具节点调度器
 * 对应Next.js版本的stopTool节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StopToolNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.STOP_TOOL.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing stop tool node: {}", node.getNodeId());
        
        try {
            // 获取当前活动工具信息
            String toolName = getStringValue(inputs, "toolName", "");
            String toolId = getStringValue(inputs, "toolId", "");
            
            // 构建停止工具响应
            Map<String, Object> response = new HashMap<>();
            response.put("stopTool", true);
            
            if (!toolName.isEmpty()) {
                response.put("toolName", toolName);
            }
            
            if (!toolId.isEmpty()) {
                response.put("toolId", toolId);
            }
            
            // 记录工具停止
            log.info("Tool stopped: name={}, id={}", toolName, toolId);
            
            // 设置结果，这将通知工作流引擎停止当前工具执行
            Map<String, Object> result = new HashMap<>();
            result.put("toolStopped", true);
            result.put("stopDetails", response);
            
            return NodeOutDTO.builder()
                .success(true)
                .nodeId(node.getNodeId())
                .output(result)
                .build();
            
        } catch (Exception e) {
            log.error("Error in stop tool node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("停止工具失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof String) {
                return (String) value;
            } else if (value != null) {
                return String.valueOf(value);
            }
        }
        return defaultValue;
    }
} 