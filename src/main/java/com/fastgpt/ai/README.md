# FastGPT Java 多模态支持

本模块提供FastGPT的Java版本多模态支持，包括文件上传和语音处理功能。

## 功能概述

### 文件处理

- **上传文件**: 支持图片和文档文件上传
- **文件存储**: 使用MongoDB GridFS存储文件内容
- **文件引用**: 在聊天消息中引用文件
- **文本提取**: 从文件中提取文本内容用于RAG

### 语音处理

- **语音转文本 (STT)**: 转录上传的音频文件
- **文本转语音 (TTS)**: 支持语音输出（配置项已添加，实现待完成）

## API端点

### 文件 API

- `POST /api/v1/files/upload`: 上传文件
- `GET /api/v1/files/{fileId}`: 获取文件
- `DELETE /api/v1/files/{fileId}`: 删除文件
- `GET /api/v1/files/{fileId}/extract`: 从文件中提取文本

### 音频 API

- `POST /api/v1/audio/transcriptions`: 转录音频文件
- `GET /api/v1/audio/models`: 获取可用STT模型

## 数据模型

### 实体类

- `ChatFile`: 文件元数据的MongoDB文档
- `ChatItem`: 支持多模态内容的聊天消息
- `ChatItemValue`: 表示聊天消息中的多模态内容（文本、图片、文件）

### 配置类

- `FileSelectConfig`: 文件选择配置
- `WhisperConfig`: 语音转文本配置
- `TTSConfig`: 文本转语音配置

## 技术实现

- 使用Spring Data MongoDB和GridFS存储文件
- 使用Spring Web提供RESTful API
- 使用OpenAI Whisper API进行语音转文本
- JWT令牌用于文件访问认证

## 数据流

1. 用户上传文件/语音
2. 服务器处理并存储内容
3. 返回文件引用/文本转录
4. 客户端在聊天中引用文件或使用转录文本

## 配置项

可通过application.properties配置：

```properties
# 文件上传配置
app.file.upload-dir=uploads
app.file.preview-url=/api/files
app.file.max-size=5242880
app.file.token-secret=your-secret-key

# OpenAI配置（用于语音转录）
spring.ai.openai.api-key=your-openai-key
spring.ai.openai.base-url=https://api.openai.com
``` 