package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.DatasetService;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索节点调度器
 * 对应Next.js版本的datasetSearchNode节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseRetrievalNodeDispatcher implements NodeDispatcher {

    private final DatasetService datasetService;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.DATASET_SEARCH_NODE.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing knowledge base retrieval node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            String query = (String) inputs.getOrDefault("query", "");
            int limit = inputs.containsKey("limit") ? 
                Integer.parseInt(String.valueOf(inputs.get("limit"))) : 5;
            
            @SuppressWarnings("unchecked")
            List<String> datasetIds = inputs.containsKey("datasetIds") && inputs.get("datasetIds") instanceof List ?
                (List<String>) inputs.get("datasetIds") : new ArrayList<>();
            
            float similarity = inputs.containsKey("similarity") ? 
                Float.parseFloat(String.valueOf(inputs.get("similarity"))) : 0.7f;
            
            // 调用数据集服务进行检索
            Map<String, Object> searchResult = datasetService.search(
                query, datasetIds, limit, similarity
            );
            
            // 处理结果
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> searchResults = searchResult.containsKey("results") && 
                searchResult.get("results") instanceof List ?
                (List<Map<String, Object>>) searchResult.get("results") : new ArrayList<>();
            
            // 构建上下文
            StringBuilder contextBuilder = new StringBuilder();
            for (Map<String, Object> result : searchResults) {
                String text = (String) result.getOrDefault("text", "");
                contextBuilder.append(text).append("\n\n");
            }
            String context = contextBuilder.toString().trim();
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("searchResults", searchResults);
            outputs.put("context", context);
            outputs.put("isEmpty", searchResults.isEmpty());
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("query", query);
            responseData.put("datasetIds", datasetIds);
            responseData.put("limit", limit);
            responseData.put("similarity", similarity);
            responseData.put("resultCount", searchResults.size());
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("datasetSearch", searchResult.getOrDefault("usage", new HashMap<>()));
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing knowledge base retrieval node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("searchResults", new ArrayList<>());
            outputs.put("context", "");
            outputs.put("isEmpty", true);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 