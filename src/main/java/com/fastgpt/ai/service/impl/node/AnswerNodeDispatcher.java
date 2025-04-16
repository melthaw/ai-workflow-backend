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
 * 回答节点处理器
 * 对应Next.js版本的dispatchAnswer函数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerNodeDispatcher implements NodeDispatcher {
    
    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.ANSWER_NODE.toString();
    }
    
    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing answer node: {}", node.getNodeId());
        
        try {
            // 获取文本内容
            String answerText = (String) inputs.getOrDefault("answerText", "");
            
            // 直接传递文本作为输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("answerText", answerText);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("text", answerText);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing answer node: {}", node.getNodeId(), e);
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("answerText", "Error: " + e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 