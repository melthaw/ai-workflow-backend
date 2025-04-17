package com.fastgpt.ai.service;

import java.io.InputStream;

/**
 * 文件存储服务接口
 * 用于处理文件读取、存储等操作
 */
public interface FileStorageService {
    
    /**
     * 获取文件内容
     * @param fileId 文件ID或路径
     * @return 文件内容输入流
     * @throws Exception 如果文件不存在或无法访问
     */
    InputStream getFileContent(String fileId) throws Exception;
    
    /**
     * 保存文件内容
     * @param fileId 文件ID或路径
     * @param content 文件内容
     * @throws Exception 如果文件无法保存
     */
    void saveFileContent(String fileId, InputStream content) throws Exception;
    
    /**
     * 检查文件是否存在
     * @param fileId 文件ID或路径
     * @return 如果文件存在返回true，否则返回false
     */
    boolean fileExists(String fileId);
    
    /**
     * 删除文件
     * @param fileId 文件ID或路径
     * @return 如果删除成功返回true，否则返回false
     */
    boolean deleteFile(String fileId);
} 