# FastGPT Java (Spring AI)

FastGPT Java 是基于 Spring Boot 和 Spring AI 开发的 FastGPT 后端实现，提供了多模态 AI 服务，包括高级聊天、工作流、文件处理、语音处理等功能。

## 功能概览

### 多模态聊天

- **文本聊天**：实现传统的文本对话功能
- **图像处理**：支持图像上传、分析和理解
- **文件处理**：支持PDF、Word、TXT等文档文件上传和内容提取
- **语音交互**：支持语音输入（STT）和语音输出（TTS）
- **多模态混合**：在同一条消息中混合使用文本、图像、文件和音频

### 工作流系统

- **可视化工作流**：基于节点图的工作流设计和执行
- **节点类型**：支持多种节点类型，包括LLM、函数调用、条件判断等
- **交互式节点**：支持需要用户输入的交互式工作流
- **模板系统**：工作流模板的创建、管理和实例化
- **调试功能**：支持工作流和单个节点的调试
- **流式输出**：工作流执行结果的实时流式返回

### 文件服务

- **文件上传**：支持多种文件类型的上传
- **文件存储**：使用MongoDB GridFS存储文件内容
- **文本提取**：自动从文档中提取文本内容
- **文件预览**：提供文件预览API
- **权限控制**：基于JWT的文件访问控制

### 音频处理

- **语音转文本**：使用Whisper API转录音频文件
- **文本转语音**：支持多种声音选项的TTS生成
- **音频流处理**：支持音频流式处理
- **多种格式**：支持MP3、WAV、WEBM等多种音频格式

### AI模型集成

- **模型配置**：灵活的AI模型配置
- **多模型支持**：集成多种LLM、嵌入、TTS、STT模型
- **参数调整**：支持模型参数的细粒度控制
- **流式响应**：支持模型的流式输出

## 系统架构

### 核心组件

- **控制器层**：处理HTTP请求和响应
- **服务层**：实现业务逻辑
- **数据访问层**：实现数据持久化
- **DTO层**：定义数据传输对象
- **实体层**：定义数据模型

### 技术栈

- **Spring Boot**：应用框架
- **Spring AI**：AI功能集成
- **MongoDB**：数据存储
- **GridFS**：文件存储
- **RestTemplate/WebClient**：外部API调用
- **JWT**：认证与授权
- **SSE**：Server-Sent Events实现流式输出

## 快速开始

### 环境要求

- JDK 17或更高版本
- Maven 3.8或更高版本
- MongoDB 5.0或更高版本

### 安装步骤

1. 克隆代码库

```bash
git clone https://github.com/yourusername/fastgpt.git
cd fastgpt/projects/spring-ai
```

2. 配置环境变量

创建`.env`文件或设置以下环境变量：

```
OPENAI_API_KEY=your_openai_api_key
MONGODB_URI=mongodb://localhost:27017/fastgpt
JWT_SECRET=your_jwt_secret
```

3. 构建项目

```bash
mvn clean package
```

4. 运行服务

```bash
java -jar target/spring-ai-0.1.0.jar
```

服务将在 [http://localhost:8080](http://localhost:8080) 上启动。

### Docker部署

1. 构建Docker镜像

```bash
docker build -t fastgpt-spring-ai .
```

2. 运行容器

```bash
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=your_openai_api_key \
  -e MONGODB_URI=mongodb://mongodb:27017/fastgpt \
  -e JWT_SECRET=your_jwt_secret \
  fastgpt-spring-ai
```

## API文档

启动服务后，可以通过访问 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) 查看完整的API文档。

### 主要API端点

#### 聊天API

- `POST /api/v1/chat`: 创建新的聊天
- `GET /api/v1/chat/{chatId}`: 获取聊天详情
- `POST /api/v1/chat/messages`: 发送消息
- `POST /api/v1/chat/messages/stream`: 发送消息并获取流式响应
- `GET /api/v1/chat/{chatId}/messages`: 获取聊天历史记录

#### 工作流API

- `POST /api/v1/workflows`: 创建工作流
- `GET /api/v1/workflows/{workflowId}`: 获取工作流详情
- `POST /api/v1/workflows/{workflowId}/execute`: 执行工作流
- `POST /api/v1/workflows/{workflowId}/stream`: 执行工作流并获取流式响应
- `GET /api/v1/workflows/templates`: 获取工作流模板

#### 文件API

- `POST /api/v1/files/upload`: 上传文件
- `GET /api/v1/files/{fileId}`: 获取文件
- `GET /api/v1/files/{fileId}/extract`: 提取文件文本内容

#### 音频API

- `POST /api/v1/audio/transcriptions`: 音频转文本
- `POST /api/v1/tts`: 文本转语音

#### AI模型API

- `GET /api/v1/ai/models`: 获取可用模型列表
- `POST /api/v1/ai/completions`: 请求模型补全
- `POST /api/v1/ai/chat/completions`: 请求聊天补全

## 配置选项

在`application.properties`或`application.yml`中可配置以下选项：

### 基础配置

```properties
# 服务器配置
server.port=8080

# 数据库配置
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/fastgpt}

# 文件上传配置
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# JWT配置
app.jwt.secret=${JWT_SECRET:defaultsecret}
app.jwt.expiration=86400
```

### AI模型配置

```properties
# OpenAI配置
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.base-url=https://api.openai.com
spring.ai.openai.chat.options.model=gpt-4-vision-preview

# 默认模型配置
app.ai.default-llm-model=gpt-3.5-turbo
app.ai.default-embedding-model=text-embedding-ada-002
app.ai.default-tts-model=tts-1
app.ai.default-stt-model=whisper-1
```

### 文件服务配置

```properties
# 文件服务配置
app.file.upload-dir=uploads
app.file.preview-url=/api/v1/files
app.file.max-size=10MB
app.file.token-secret=your-file-token-secret
app.file.token-expiration=3600
```

### 音频服务配置

```properties
# 音频服务配置
app.audio.default-model=whisper-1
app.audio.max-duration=300

# TTS配置
app.tts.default-model=tts-1
app.tts.default-voice=alloy
```

## 架构扩展

### 添加新的模型提供商

1. 创建提供商配置类

```java
@Configuration
@ConfigurationProperties(prefix = "app.ai.newprovider")
public class NewProviderConfig {
    private String apiKey;
    private String baseUrl;
    // getters and setters
}
```

2. 创建模型实现类

```java
@Service
public class NewProviderModelService {
    private final NewProviderConfig config;
    
    public NewProviderModelService(NewProviderConfig config) {
        this.config = config;
    }
    
    // 实现模型方法
}
```

3. 在配置文件中添加相关配置

```properties
app.ai.newprovider.api-key=${NEWPROVIDER_API_KEY}
app.ai.newprovider.base-url=https://api.newprovider.com
```

### 添加新的节点类型

1. 创建节点处理器

```java
@Component
public class CustomNodeHandler implements NodeHandler {
    @Override
    public String getType() {
        return "customNode";
    }
    
    @Override
    public NodeOutDTO execute(WorkflowDTO workflow, String nodeId, Map<String, Object> inputs) {
        // 实现节点逻辑
    }
}
```

2. 注册节点处理器

```java
@Configuration
public class NodeHandlerConfig {
    @Bean
    public NodeHandlerRegistry nodeHandlerRegistry(List<NodeHandler> handlers) {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        handlers.forEach(registry::register);
        return registry;
    }
}
```

## 性能优化

### JVM优化

```bash
java -Xms1G -Xmx4G -XX:+UseG1GC -jar target/spring-ai-0.1.0.jar
```

### 数据库索引

确保MongoDB中创建了适当的索引：

```javascript
db.chats.createIndex({ "chatId": 1 });
db.chats.createIndex({ "userId": 1 });
db.chats.createIndex({ "appId": 1 });
db.workflows.createIndex({ "workflowId": 1 });
```

### 连接池配置

```properties
spring.data.mongodb.connection-pool-max-size=100
spring.data.mongodb.connection-pool-min-size=5
```

## 运行测试

```bash
mvn test
```

要运行特定测试：

```bash
mvn test -Dtest=ChatServiceTest
```

## 贡献指南

1. Fork代码库
2. 创建功能分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -m 'Add some feature'`)
4. 推送到分支 (`git push origin feature/your-feature`)
5. 创建Pull Request

## 许可证

本项目基于[MIT许可证](LICENSE)进行许可。 