package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * HTML提取节点调度器
 * 对应Next.js版本的htmlExtract节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HtmlExtractNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.HTML_EXTRACT.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing HTML extract node: {}", node.getNodeId());
        
        try {
            // 获取输入HTML内容
            String html = getStringValue(inputs, "html", "");
            
            if (html.isEmpty()) {
                return NodeOutDTO.error("没有要提取的HTML内容");
            }
            
            // 使用JSoup解析HTML
            Document doc = Jsoup.parse(html);
            
            // 提取标题
            String title = doc.title();
            
            // 提取纯文本内容
            String content = doc.text();
            
            // 创建结果Map
            Map<String, Object> result = new HashMap<>();
            result.put("title", title);
            result.put("content", content);
            
            return NodeOutDTO.success(result);
            
        } catch (Exception e) {
            log.error("Error in HTML extract node: {}", e.getMessage(), e);
            
            return NodeOutDTO.error("HTML提取失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取字符串值，带默认值
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof String) {
                return (String) value;
            }
            return String.valueOf(value);
        }
        return defaultValue;
    }
} 