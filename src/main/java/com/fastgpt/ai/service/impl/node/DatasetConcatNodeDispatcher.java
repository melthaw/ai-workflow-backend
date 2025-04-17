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
 * 数据集拼接节点调度器
 * 对应Next.js版本的datasetConcatNode节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetConcatNodeDispatcher implements NodeDispatcher {

    private final DatasetService datasetService;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.DATASET_CONCAT_NODE.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing dataset concat node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            @SuppressWarnings("unchecked")
            List<String> datasetIds = inputs.containsKey("datasetIds") && inputs.get("datasetIds") instanceof List ?
                (List<String>) inputs.get("datasetIds") : new ArrayList<>();
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> searchResults = inputs.containsKey("searchResults") && 
                inputs.get("searchResults") instanceof List ?
                (List<Map<String, Object>>) inputs.get("searchResults") : new ArrayList<>();
            
            // 调用数据集服务进行合并
            Map<String, Object> concatResult = datasetService.concat(datasetIds, searchResults);
            
            // 处理结果
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> combinedResults = concatResult.containsKey("results") && 
                concatResult.get("results") instanceof List ?
                (List<Map<String, Object>>) concatResult.get("results") : new ArrayList<>();
            
            // 构建上下文
            StringBuilder contextBuilder = new StringBuilder();
            for (Map<String, Object> result : combinedResults) {
                String text = (String) result.getOrDefault("text", "");
                contextBuilder.append(text).append("\n\n");
            }
            String context = contextBuilder.toString().trim();
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("searchResults", combinedResults);
            outputs.put("context", context);
            outputs.put("isEmpty", combinedResults.isEmpty());
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("datasetIds", datasetIds);
            responseData.put("inputResultsCount", searchResults.size());
            responseData.put("outputResultsCount", combinedResults.size());
            
            // 使用情况统计
            Map<String, Object> usages = new HashMap<>();
            usages.put("datasetConcat", concatResult.getOrDefault("usage", new HashMap<>()));
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing dataset concat node: {}", node.getNodeId(), e);
            
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