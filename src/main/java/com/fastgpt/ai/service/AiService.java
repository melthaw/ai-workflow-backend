package com.fastgpt.ai.service;

import java.util.List;
import java.util.Map;

/**
 * AI服务接口
 * 统一处理各种AI模型调用
 */
public interface AiService {
    
    /**
     * 聊天对话
     *
     * @param prompt 对话内容
     * @param systemPrompt 系统提示
     * @param history 对话历史
     * @param model 模型名称
     * @return 对话结果
     */
    Map<String, Object> chat(String prompt, String systemPrompt, List<Map<String, String>> history, String model);
    
    /**
     * 文本生成
     *
     * @param prompt 生成提示
     * @param model 模型名称
     * @param options 生成选项
     * @return 生成结果
     */
    Map<String, Object> generateText(String prompt, String model, Map<String, Object> options);
    
    /**
     * 文本嵌入
     *
     * @param text 文本内容
     * @param model 模型名称
     * @return 嵌入向量
     */
    Map<String, Object> embedText(String text, String model);
    
    /**
     * 函数调用
     *
     * @param prompt 提示内容
     * @param functions 可用函数定义
     * @param model 模型名称
     * @return 函数调用结果
     */
    Map<String, Object> functionCall(String prompt, List<Map<String, Object>> functions, String model);
    
    /**
     * 文本分类
     *
     * @param text 待分类文本
     * @param categories 分类类别
     * @param customPrompt 自定义提示词
     * @return 分类结果
     */
    Map<String, Object> classifyText(String text, List<String> categories, String customPrompt);
    
    /**
     * 情感分析
     *
     * @param text 文本内容
     * @param options 分析选项
     * @return 情感分析结果
     */
    Map<String, Object> analyzeSentiment(String text, Map<String, Object> options);
    
    /**
     * 生成简单文本响应
     * 
     * @param prompt 提示词
     * @param model 模型名称，如果为null则使用默认模型
     * @return 生成的文本响应
     */
    String generateSimpleResponse(String prompt, String model);
} 