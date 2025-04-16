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
 * 查询扩展节点处理器
 * 对应Next.js版本的dispatchQueryExtension函数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExtensionNodeDispatcher implements NodeDispatcher {

    private final AiService aiService;
    
    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.QUERY_EXTENSION_NODE.toString();
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing query extension node: {}", node.getNodeId());
        
        try {
            // 获取原始查询
            String query = (String) inputs.getOrDefault("query", "");
            int maxCount = inputs.containsKey("maxCount") ? 
                Integer.parseInt(String.valueOf(inputs.get("maxCount"))) : 3;
            
            // 调用AI服务扩展查询
            List<String> extendedQueries = generateExtendedQueries(query, maxCount);
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("extendedQueries", extendedQueries);
            outputs.put("query", query);
            outputs.put("success", true);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("originalQuery", query);
            responseData.put("extendedQueries", extendedQueries);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing query extension node: {}", node.getNodeId(), e);
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("extendedQueries", new ArrayList<>());
            outputs.put("query", inputs.getOrDefault("query", ""));
            outputs.put("success", false);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 生成扩展查询
     * 实际实现中应使用AI服务生成相关查询
     */
    private List<String> generateExtendedQueries(String query, int maxCount) {
        // 模拟实现，实际应调用AI服务
        List<String> extendedQueries = new ArrayList<>();
        extendedQueries.add(query + " 详细信息");
        extendedQueries.add("如何理解 " + query);
        
        if (maxCount > 2) {
            extendedQueries.add(query + " 的实际应用");
        }
        
        if (maxCount > 3) {
            extendedQueries.add(query + " 常见问题");
        }
        
        return extendedQueries;
    }
} 