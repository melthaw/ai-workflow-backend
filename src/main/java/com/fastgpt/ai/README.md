# FastGPT Java 多模态支持

本模块提供FastGPT的Java版本多模态支持，包括文件处理、图像处理和语音处理功能。支持与Next.js版本相同的多模态交互体验。

## 功能概述

### 文件处理

- **上传文件**: 支持图片、PDF、Word、TXT等文档文件上传
- **文件存储**: 使用MongoDB GridFS存储文件内容，保证扩展性
- **文件引用**: 在聊天消息中引用文件，支持多文件关联
- **文本提取**: 自动从文件中提取文本内容用于RAG检索
- **文件预览**: 提供文件预览API，支持在线查看

### 图像处理

- **图像分析**: 支持图像理解和分析
- **图像转文本**: 提取图像中的文本信息
- **图像描述**: 生成图像的详细描述
- **多图上传**: 支持在一条消息中上传多张图片

### 语音处理

- **语音转文本 (STT)**: 使用Whisper API转录上传的音频文件
- **文本转语音 (TTS)**: 支持语音输出，可配置模型和声音
- **音频流处理**: 支持音频流式处理
- **多种音频格式**: 支持MP3、WAV、WEBM等多种音频格式

## 技术实现

### 多模态核心架构

```
MultiModalService
    ├── FileService (文件处理)
    ├── AudioService (音频处理)
    └── TTSService (文本转语音)
```

### 多模态消息结构

```json
{
  "obj": "user",
  "value": [
    {
      "type": "text",
      "text": "用户输入的文本"
    },
    {
      "type": "image",
      "url": "图片URL",
      "name": "图片名称"
    },
    {
      "type": "file",
      "url": "文件URL",
      "name": "文件名称"
    },
    {
      "type": "audio",
      "url": "音频URL",
      "duration": 30,
      "transcription": "转录文本"
    }
  ]
}
```

### 数据流程

1. **上传阶段**:
   - 文件上传至API服务器
   - 存储到MongoDB GridFS
   - 视类型执行处理(图像分析、音频转录等)
   - 返回文件引用信息

2. **消息阶段**:
   - 构建多模态消息，包含文本和文件引用
   - 发送给AI服务处理
   - 根据AI能力处理多模态内容

3. **响应阶段**:
   - 接收AI响应
   - 处理返回的文本和多模态内容
   - 返回给客户端

## API端点

### 文件 API

- `POST /api/v1/files/upload`: 上传文件
  ```
  请求参数:
  - file: 文件内容 (MultipartFile)
  - chatId: 聊天ID (可选)
  - appId: 应用ID (可选)
  - userId: 用户ID
  ```

- `GET /api/v1/files/{fileId}`: 获取文件
  ```
  路径参数:
  - fileId: 文件ID
  
  查询参数:
  - token: 访问令牌 (可选)
  ```

- `DELETE /api/v1/files/{fileId}`: 删除文件
  ```
  路径参数:
  - fileId: 文件ID
  ```

- `GET /api/v1/files/{fileId}/extract`: 从文件中提取文本
  ```
  路径参数:
  - fileId: 文件ID
  ```

### 音频 API

- `POST /api/v1/audio/transcriptions`: 转录音频文件
  ```
  请求参数:
  - file: 音频文件 (MultipartFile)
  - model: 转录模型 (可选，默认为whisper-1)
  - userId: 用户ID
  - chatId: 聊天ID (可选)
  - appId: 应用ID (可选)
  ```

- `GET /api/v1/audio/models`: 获取可用STT模型
  ```
  返回:
  - 可用模型数组
  ```

- `POST /api/v1/tts`: 文本转语音
  ```
  请求参数:
  - text: 要转换的文本
  - model: TTS模型 (可选)
  - voice: 声音类型 (可选)
  - userId: 用户ID
  ```

## 实现原理

### 文件处理

1. 使用`GridFsTemplate`存储上传的文件
2. 使用文件类型检测确定内容类型
3. 对于文档类型，使用Apache Tika提取文本
4. 使用JWT令牌保护文件访问

### 图像处理

1. 支持常见图像格式(JPEG, PNG, GIF, WEBP等)
2. 将图像URL以多模态格式发送给AI
3. 通过适当的提示引导AI理解图像内容

### 语音处理

1. 将音频文件转换为合适格式
2. 使用OpenAI Whisper API进行转录
3. 将转录文本与原始音频关联
4. 支持文本转语音生成音频响应

## 使用示例

### 上传和处理图像

```java
// 上传图像文件
MultipartFile imageFile = getImageFile();
FileUploadDTO uploadResult = fileService.uploadFile(
    imageFile, chatId, appId, userId, null);

// 创建多模态消息
ChatItemValue textValue = multiModalService.createTextValue("请描述这张图片");
ChatItemValue imageValue = multiModalService.createFileValue(
    "image", uploadResult.getName(), uploadResult.getUrl());
List<ChatItemValue> values = Arrays.asList(textValue, imageValue);

// 发送消息
ChatMessageRequest request = ChatMessageRequest.builder()
    .chatId(chatId)
    .appId(appId)
    .userId(userId)
    .value(values)
    .build();

ChatItemDTO response = chatService.sendMessage(request);
```

### 语音转文本

```java
// 上传音频文件
MultipartFile audioFile = getAudioFile();
AudioTranscriptionResponse transcription = audioService.transcribeAudio(
    audioFile, "whisper-1", userId, chatId, appId);

// 使用转录文本
String text = transcription.getText();
System.out.println("转录结果: " + text);
```

## 配置项

可通过application.properties配置：

```properties
# 文件上传配置
app.file.upload-dir=uploads
app.file.preview-url=/api/v1/files
app.file.max-size=10MB
app.file.token-secret=your-secret-key
app.file.token-expiration=3600

# OpenAI配置（用于多模态处理）
spring.ai.openai.api-key=your-openai-key
spring.ai.openai.base-url=https://api.openai.com
spring.ai.openai.chat.options.model=gpt-4-vision-preview

# 语音转录配置
app.audio.default-model=whisper-1
app.audio.max-duration=300

# 文本转语音配置
app.tts.default-model=tts-1
app.tts.default-voice=alloy
```

## 最佳实践

1. **文件大小限制**: 限制上传文件大小，避免过大文件
2. **图像分辨率**: 发送给AI的图像应适当压缩
3. **音频长度**: 控制音频长度，避免超出服务限制
4. **处理失败**: 实现适当的错误处理和重试机制
5. **文件清理**: 定期清理未被引用的文件 