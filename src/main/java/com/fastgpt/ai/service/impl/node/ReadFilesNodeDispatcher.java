package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.FileStorageService;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 读取文件节点调度器
 * 对应Next.js版本的readFiles节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadFilesNodeDispatcher implements NodeDispatcher {

    private final FileStorageService fileStorageService;

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.READ_FILES.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing read files node: {}", node.getNodeId());
        
        try {
            // 获取文件URL列表
            List<String> fileUrls = getListValue(inputs, "fileUrlList", new ArrayList<>());
            
            if (fileUrls.isEmpty()) {
                return NodeOutDTO.builder()
                    .success(false)
                    .nodeId(node.getNodeId())
                    .error("没有指定要读取的文件")
                    .build();
            }
            
            // 读取所有文件内容
            List<Map<String, Object>> fileContents = new ArrayList<>();
            
            for (String fileUrl : fileUrls) {
                try {
                    // 处理文件URL，获取文件路径或ID
                    String fileId = extractFileId(fileUrl);
                    
                    // 从存储服务获取文件内容
                    String content = readFileContent(fileId);
                    
                    // 添加到结果列表
                    Map<String, Object> fileData = new HashMap<>();
                    fileData.put("url", fileUrl);
                    fileData.put("content", content);
                    fileData.put("name", getFileNameFromUrl(fileUrl));
                    fileContents.add(fileData);
                    
                } catch (Exception e) {
                    log.error("Error reading file {}: {}", fileUrl, e.getMessage());
                    
                    // 添加错误信息到结果
                    Map<String, Object> fileData = new HashMap<>();
                    fileData.put("url", fileUrl);
                    fileData.put("content", "");
                    fileData.put("name", getFileNameFromUrl(fileUrl));
                    fileData.put("error", "读取文件失败: " + e.getMessage());
                    fileContents.add(fileData);
                }
            }
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("files", fileContents);
            
            // 合并所有文件内容
            String allContent = fileContents.stream()
                .map(file -> file.get("content").toString())
                .collect(Collectors.joining("\n\n"));
            
            result.put("content", allContent);
            
            return NodeOutDTO.builder()
                .success(true)
                .nodeId(node.getNodeId())
                .output(result)
                .build();
            
        } catch (Exception e) {
            log.error("Error in read files node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("读取文件失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 从URL中提取文件ID
     */
    private String extractFileId(String fileUrl) {
        // 根据实际URL格式提取文件ID
        // 例如，从URL中获取文件路径部分或者ID部分
        
        // 示例实现，根据实际情况修改
        if (fileUrl.contains("/files/")) {
            return fileUrl.substring(fileUrl.lastIndexOf("/files/") + 7);
        }
        
        return fileUrl;
    }
    
    /**
     * 从URL中获取文件名
     */
    private String getFileNameFromUrl(String fileUrl) {
        // 从URL中提取文件名
        int lastSlash = fileUrl.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < fileUrl.length() - 1) {
            String fileName = fileUrl.substring(lastSlash + 1);
            
            // 处理可能的查询参数
            int queryIndex = fileName.indexOf('?');
            if (queryIndex > 0) {
                fileName = fileName.substring(0, queryIndex);
            }
            
            return fileName;
        }
        
        return "unknown";
    }
    
    /**
     * 读取文件内容
     */
    private String readFileContent(String fileId) throws Exception {
        // 使用文件存储服务读取内容
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(fileStorageService.getFileContent(fileId)))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * 获取列表值，带默认值
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getListValue(Map<String, Object> map, String key, List<T> defaultValue) {
        if (map != null && map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof List) {
                return (List<T>) value;
            }
            // 处理单个值
            if (value != null) {
                List<T> result = new ArrayList<>();
                try {
                    result.add((T) value);
                    return result;
                } catch (ClassCastException e) {
                    // 无法转换，返回默认值
                }
            }
        }
        return defaultValue;
    }
} 