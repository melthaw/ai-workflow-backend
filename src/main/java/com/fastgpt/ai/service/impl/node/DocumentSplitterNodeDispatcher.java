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
import java.util.regex.Pattern;

/**
 * 文档分割节点调度器
 * 将长文本分割成较小的chunk
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSplitterNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return "documentSplitter";
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing document splitter node: {}", node.getNodeId());
        
        try {
            // 获取输入参数
            String text = (String) inputs.getOrDefault("text", "");
            
            // 获取分割参数
            String splitBy = (String) inputs.getOrDefault("splitBy", "paragraph");
            int chunkSize = inputs.containsKey("chunkSize") ? 
                Integer.parseInt(String.valueOf(inputs.get("chunkSize"))) : 500;
            int chunkOverlap = inputs.containsKey("chunkOverlap") ? 
                Integer.parseInt(String.valueOf(inputs.get("chunkOverlap"))) : 50;
            
            // 分割文档
            List<Map<String, Object>> chunks = splitDocument(text, splitBy, chunkSize, chunkOverlap);
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("chunks", chunks);
            outputs.put("chunkCount", chunks.size());
            outputs.put("totalLength", text.length());
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("splitBy", splitBy);
            responseData.put("chunkSize", chunkSize);
            responseData.put("chunkOverlap", chunkOverlap);
            responseData.put("chunkCount", chunks.size());
            responseData.put("totalLength", text.length());
            
            // 使用情况统计
            Map<String, Object> usage = new HashMap<>();
            usage.put("tokens", text.length() / 4);
            
            Map<String, Object> usages = new HashMap<>();
            usages.put("documentSplitter", usage);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .usages(usages)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing document splitter node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("chunks", new ArrayList<>());
            outputs.put("chunkCount", 0);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 分割文档
     */
    private List<Map<String, Object>> splitDocument(String text, String splitBy, int chunkSize, int chunkOverlap) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // 根据不同方式分割
        List<String> textChunks = new ArrayList<>();
        
        switch (splitBy) {
            case "paragraph":
                // 按段落分割（空行）
                String[] paragraphs = text.split("\\n\\s*\\n");
                for (String paragraph : paragraphs) {
                    if (!paragraph.trim().isEmpty()) {
                        textChunks.add(paragraph.trim());
                    }
                }
                break;
                
            case "sentence":
                // 按句子分割（句号、问号、感叹号）
                String[] sentences = text.split("[.!?。？！]");
                for (String sentence : sentences) {
                    if (!sentence.trim().isEmpty()) {
                        textChunks.add(sentence.trim() + ".");
                    }
                }
                break;
                
            case "character":
                // 按字符数量分割
                for (int i = 0; i < text.length(); i += chunkSize - chunkOverlap) {
                    int end = Math.min(i + chunkSize, text.length());
                    textChunks.add(text.substring(i, end));
                }
                break;
                
            case "token":
                // 按估算的token数量分割（这里用字符数/4简单估算）
                int tokensPerChunk = chunkSize;
                int charsPerChunk = tokensPerChunk * 4;
                for (int i = 0; i < text.length(); i += charsPerChunk - (chunkOverlap * 4)) {
                    int end = Math.min(i + charsPerChunk, text.length());
                    textChunks.add(text.substring(i, end));
                }
                break;
                
            default:
                // 默认按段落分割
                String[] defaultParagraphs = text.split("\\n\\s*\\n");
                for (String paragraph : defaultParagraphs) {
                    if (!paragraph.trim().isEmpty()) {
                        textChunks.add(paragraph.trim());
                    }
                }
        }
        
        // 创建chunk对象
        for (int i = 0; i < textChunks.size(); i++) {
            Map<String, Object> chunk = new HashMap<>();
            chunk.put("text", textChunks.get(i));
            chunk.put("index", i);
            chunk.put("length", textChunks.get(i).length());
            
            chunks.add(chunk);
        }
        
        return chunks;
    }
} 