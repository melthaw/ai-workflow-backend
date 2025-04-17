package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AI服务接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final Random random = new Random();

    @Override
    public Map<String, Object> chat(String prompt, String systemPrompt, List<Map<String, String>> history, String model) {
        log.info("Chat request, model: {}, prompt: {}", model, prompt);
        
        // 模拟聊天实现
        Map<String, Object> response = new HashMap<>();
        response.put("content", "这是来自AI的回复: " + prompt);
        
        Map<String, Object> usage = new HashMap<>();
        usage.put("prompt_tokens", prompt.length() / 4);
        usage.put("completion_tokens", 20);
        usage.put("total_tokens", prompt.length() / 4 + 20);
        response.put("usage", usage);
        
        return response;
    }

    @Override
    public Map<String, Object> generateText(String prompt, String model, Map<String, Object> options) {
        log.info("Text generation request, model: {}, prompt: {}", model, prompt);
        
        // 文本生成模拟
        Map<String, Object> response = new HashMap<>();
        response.put("text", "生成的文本内容: " + prompt);
        
        Map<String, Object> usage = new HashMap<>();
        usage.put("tokens", prompt.length() / 4 + 10);
        response.put("usage", usage);
        
        return response;
    }

    @Override
    public Map<String, Object> embedText(String text, String model) {
        log.info("Text embedding request, model: {}, text length: {}", model, text.length());
        
        // 嵌入向量模拟
        Map<String, Object> response = new HashMap<>();
        
        // 生成随机向量
        List<Double> vector = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            vector.add(random.nextDouble());
        }
        
        response.put("embedding", vector);
        
        Map<String, Object> usage = new HashMap<>();
        usage.put("tokens", text.length() / 4);
        response.put("usage", usage);
        
        return response;
    }

    @Override
    public Map<String, Object> functionCall(String prompt, List<Map<String, Object>> functions, String model) {
        log.info("Function call request, model: {}, prompt: {}", model, prompt);
        
        // 函数调用模拟
        Map<String, Object> response = new HashMap<>();
        
        if (functions != null && !functions.isEmpty()) {
            Map<String, Object> function = functions.get(0);
            
            Map<String, Object> functionCall = new HashMap<>();
            functionCall.put("name", function.get("name"));
            functionCall.put("arguments", "{}");
            
            response.put("functionCall", functionCall);
        }
        
        Map<String, Object> usage = new HashMap<>();
        usage.put("tokens", prompt.length() / 4 + 15);
        response.put("usage", usage);
        
        return response;
    }

    @Override
    public Map<String, Object> classifyText(String text, List<String> categories, String customPrompt) {
        log.info("Text classification request, text length: {}, categories: {}", text.length(), categories);
        
        // 文本分类模拟
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> classification = new HashMap<>();
        if (categories != null && !categories.isEmpty()) {
            classification.put("category", categories.get(0));
            classification.put("confidence", 0.8);
        } else {
            classification.put("category", "其他");
            classification.put("confidence", 0.5);
        }
        
        response.put("classification", classification);
        
        Map<String, Object> usage = new HashMap<>();
        usage.put("tokens", text.length() / 4);
        response.put("usage", usage);
        
        return response;
    }

    @Override
    public Map<String, Object> analyzeSentiment(String text, Map<String, Object> options) {
        log.info("Sentiment analysis request, text length: {}", text.length());
        
        // 情感分析模拟
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> sentiment = new HashMap<>();
        sentiment.put("sentiment", "positive");
        sentiment.put("score", 0.7);
        
        Map<String, Object> details = new HashMap<>();
        details.put("positive", 0.7);
        details.put("neutral", 0.2);
        details.put("negative", 0.1);
        sentiment.put("details", details);
        
        response.put("sentiment", sentiment);
        
        Map<String, Object> usage = new HashMap<>();
        usage.put("tokens", text.length() / 4);
        response.put("usage", usage);
        
        return response;
    }

    /**
     * 生成简单文本响应
     * 
     * @param prompt 提示词
     * @param model 模型名称，如果为null则使用默认模型
     * @return 生成的文本响应
     */
    @Override
    public String generateSimpleResponse(String prompt, String model) {
        // 实际实现中应该调用AI服务API
        // 这里是简化实现，实际项目应替换为真实实现
        log.info("Generating AI response with prompt: {}, model: {}", prompt, model);
        
        // 模拟AI生成结果，实际项目应调用真实AI服务
        return simulateAiResponse(prompt);
    }
    
    /**
     * 模拟AI响应，仅供测试
     */
    private String simulateAiResponse(String prompt) {
        // 简单逻辑：根据提示词中的关键字返回不同结果
        if (prompt.contains("JSON") || prompt.contains("json")) {
            // 如果提示要求JSON格式，返回一个JSON示例
            return "{\n  \"result\": \"This is a simulated AI response\",\n  \"status\": \"success\"\n}";
        } else if (prompt.contains("extract") || prompt.contains("提取")) {
            // 如果是提取内容的提示
            return "Extracted content from the provided text";
        } else {
            // 默认响应
            return "This is a simulated AI response to your prompt: " + prompt.substring(0, Math.min(50, prompt.length())) + "...";
        }
    }
}
