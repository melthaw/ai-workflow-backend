package com.fastgpt.ai.service.impl.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求节点调度器
 * 对应Next.js版本的httpRequest468节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpRequestNodeDispatcher implements NodeDispatcher {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.HTTP_REQUEST_468.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing HTTP request node: {}", node.getNodeId());
        
        try {
            // 获取请求参数
            String url = (String) inputs.getOrDefault("url", "");
            String method = (String) inputs.getOrDefault("method", "GET");
            
            // 构建请求头
            @SuppressWarnings("unchecked")
            Map<String, String> headers = inputs.containsKey("headers") && 
                inputs.get("headers") instanceof Map ?
                (Map<String, String>) inputs.get("headers") : new HashMap<>();
            
            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::add);
            
            // 获取请求体
            Object body = inputs.get("body");
            String requestBody = null;
            
            if (body instanceof String) {
                requestBody = (String) body;
            } else if (body instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap = (Map<String, Object>) body;
                requestBody = objectMapper.writeValueAsString(bodyMap);
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            }
            
            // 设置超时参数
            int timeout = inputs.containsKey("timeout") ? 
                Integer.parseInt(String.valueOf(inputs.get("timeout"))) : 
                30000; // 默认30秒
            
            // 执行HTTP请求
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(method.toUpperCase()),
                requestEntity,
                String.class
            );
            long endTime = System.currentTimeMillis();
            
            // 处理响应
            String responseBody = response.getBody();
            int statusCode = response.getStatusCode().value();
            
            // 尝试解析JSON响应
            Map<String, Object> parsedResponse = new HashMap<>();
            try {
                if (responseBody != null && !responseBody.isEmpty()) {
                    if (responseBody.trim().startsWith("{") || responseBody.trim().startsWith("[")) {
                        parsedResponse = objectMapper.readValue(responseBody, Map.class);
                    } else {
                        // 非JSON响应作为文本处理
                        parsedResponse.put("text", responseBody);
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to parse response as JSON: {}", e.getMessage());
                parsedResponse.put("text", responseBody);
            }
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("status", statusCode);
            outputs.put("data", parsedResponse);
            outputs.put("headers", response.getHeaders());
            outputs.put("success", statusCode >= 200 && statusCode < 300);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("url", url);
            responseData.put("method", method);
            responseData.put("statusCode", statusCode);
            responseData.put("executionTimeMs", endTime - startTime);
            responseData.put("responseSize", responseBody != null ? responseBody.length() : 0);
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .responseData(responseData)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing HTTP request node: {}", node.getNodeId(), e);
            
            // 构建错误输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("status", 500);
            outputs.put("data", new HashMap<>());
            outputs.put("headers", new HashMap<>());
            outputs.put("success", false);
            outputs.put("error", e.getMessage());
            
            return NodeOutDTO.builder()
                    .output(outputs)
                    .error(e.getMessage())
                    .build();
        }
    }
} 