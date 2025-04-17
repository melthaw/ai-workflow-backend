package com.fastgpt.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 数据集服务接口
 */
public interface DatasetService {
    
    /**
     * 在数据集中搜索
     *
     * @param query 查询字符串
     * @param datasetIds 数据集ID列表
     * @param limit 最大返回结果数
     * @param similarity 相似度阈值
     * @return 搜索结果和使用统计
     */
    Map<String, Object> search(String query, List<String> datasetIds, int limit, float similarity);
    
    /**
     * 合并多个数据集的搜索结果
     *
     * @param datasetIds 数据集ID列表
     * @param searchResults 多个搜索结果
     * @return 合并后的结果
     */
    Map<String, Object> concat(List<String> datasetIds, List<Map<String, Object>> searchResults);
} 