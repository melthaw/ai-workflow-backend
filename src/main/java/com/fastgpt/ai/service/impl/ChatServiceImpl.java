package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.ChatDTO;
import com.fastgpt.ai.dto.ChatItemDTO;
import com.fastgpt.ai.dto.KbDataDTO;
import com.fastgpt.ai.dto.request.ChatCreateRequest;
import com.fastgpt.ai.dto.request.ChatMessageRequest;
import com.fastgpt.ai.entity.App;
import com.fastgpt.ai.entity.Chat;
import com.fastgpt.ai.entity.ChatItem;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.mapper.ChatItemMapper;
import com.fastgpt.ai.mapper.ChatMapper;
import com.fastgpt.ai.repository.AppRepository;
import com.fastgpt.ai.repository.ChatItemRepository;
import com.fastgpt.ai.repository.ChatRepository;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.ChatConfigService;
import com.fastgpt.ai.service.ChatService;
import com.fastgpt.ai.service.RagService;
import com.fastgpt.ai.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatItemRepository chatItemRepository;
    private final AppRepository appRepository;
    private final ChatMapper chatMapper;
    private final ChatItemMapper chatItemMapper;
    private final AiService aiService;
    private final ChatConfigService chatConfigService;
    private final WorkflowService workflowService;
    private final RagService ragService;

    @Override
    @Transactional
    public ChatDTO createChat(ChatCreateRequest request) {
        // Verify app exists
        App app = appRepository.findByAppId(request.getAppId())
                .orElseThrow(() -> new ResourceNotFoundException("App", "appId", request.getAppId()));
                
        Chat chat = chatMapper.toEntity(request);
        
        // Generate a unique chatId
        chat.setChatId(UUID.randomUUID().toString());
        chat.setUpdateTime(LocalDateTime.now());
        chat.setTop(false);
        
        // Set default title if not provided
        if (chat.getTitle() == null) {
            chat.setTitle("New Chat");
        }
        
        Chat savedChat = chatRepository.save(chat);
        
        // If welcome text is provided, create a system message
        if (StringUtils.hasText(request.getWelcomeText())) {
            ChatItem welcomeMessage = new ChatItem();
            welcomeMessage.setChatId(savedChat.getChatId());
            welcomeMessage.setUserId(request.getUserId());
            welcomeMessage.setTeamId(request.getTeamId());
            welcomeMessage.setTmbId(request.getTmbId());
            welcomeMessage.setAppId(request.getAppId());
            welcomeMessage.setTime(LocalDateTime.now());
            welcomeMessage.setObj("system");
            welcomeMessage.setValue(request.getWelcomeText());
            chatItemRepository.save(welcomeMessage);
        }
        // Use app's system prompt if no welcome text provided
        else if (StringUtils.hasText(app.getSystemPrompt())) {
            ChatItem systemMessage = new ChatItem();
            systemMessage.setChatId(savedChat.getChatId());
            systemMessage.setUserId(request.getUserId());
            systemMessage.setTeamId(request.getTeamId());
            systemMessage.setTmbId(request.getTmbId());
            systemMessage.setAppId(request.getAppId());
            systemMessage.setTime(LocalDateTime.now());
            systemMessage.setObj("system");
            systemMessage.setValue(app.getSystemPrompt());
            chatItemRepository.save(systemMessage);
        }
        
        return chatMapper.toDTO(savedChat);
    }

    @Override
    public ChatDTO getChatById(String chatId) {
        return chatRepository.findByChatId(chatId)
                .map(chatMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Chat", "chatId", chatId));
    }

    @Override
    public List<ChatDTO> getChatsByUserId(String userId) {
        return chatMapper.toDTOList(chatRepository.findByUserId(userId));
    }

    @Override
    public List<ChatDTO> getChatsByAppId(String appId) {
        return chatMapper.toDTOList(chatRepository.findByAppId(appId));
    }

    @Override
    public List<ChatDTO> getChatsByUserIdAndAppId(String userId, String appId) {
        return chatMapper.toDTOList(chatRepository.findByUserIdAndAppId(userId, appId));
    }

    @Override
    @Transactional
    public void deleteChat(String chatId) {
        if (!chatRepository.findByChatId(chatId).isPresent()) {
            throw new ResourceNotFoundException("Chat", "chatId", chatId);
        }
        chatItemRepository.deleteByChatId(chatId);
        chatRepository.deleteByChatId(chatId);
    }

    @Override
    @Transactional
    public ChatItemDTO sendMessage(ChatMessageRequest request) {
        // Verify chat exists
        Chat chat = chatRepository.findByChatId(request.getChatId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat", "chatId", request.getChatId()));
        
        // Update chat's last update time
        chat.setUpdateTime(LocalDateTime.now());
        chatRepository.save(chat);
        
        // Save user message
        ChatItem userChatItem = new ChatItem();
        userChatItem.setChatId(request.getChatId());
        userChatItem.setUserId(request.getUserId());
        userChatItem.setTeamId(request.getTeamId());
        userChatItem.setTmbId(request.getTmbId());
        userChatItem.setAppId(request.getAppId());
        userChatItem.setTime(LocalDateTime.now());
        userChatItem.setObj("user");
        userChatItem.setValue(request.getMessage());
        userChatItem.setMetadata(request.getMetadata());
        chatItemRepository.save(userChatItem);
        
        try {
            // Get app for configurations
            Optional<App> appOptional = Optional.empty();
            if (StringUtils.hasText(request.getAppId())) {
                appOptional = appRepository.findByAppId(request.getAppId());
            }
            
            // Check if app uses workflow
            if (appOptional.isPresent() && appOptional.get().getUseWorkflow() != null && 
                    appOptional.get().getUseWorkflow() && 
                    StringUtils.hasText(appOptional.get().getWorkflowId())) {
                
                // Use workflow for processing
                return processWithWorkflow(request, chat, appOptional.get(), userChatItem);
            } 
            // Check if explicit RAG is requested in the request
            else if (request.getUseRag() != null && request.getUseRag() && 
                    request.getKbIds() != null && !request.getKbIds().isEmpty()) {
                
                // Use RAG with request-specified knowledge bases
                return processWithRag(request, chat, appOptional, userChatItem);
            }
            // Check if app has configured knowledge bases
            else if (appOptional.isPresent() && appOptional.get().getUseKb() != null && 
                    appOptional.get().getUseKb() && 
                    appOptional.get().getKbIds() != null && 
                    !appOptional.get().getKbIds().isEmpty()) {
                
                // Use RAG with app-configured knowledge bases
                return processWithAppRag(request, chat, appOptional.get(), userChatItem);
            }
            else {
                // Use standard AI processing
                return processWithAI(request, chat, appOptional);
            }
        } catch (Exception e) {
            log.error("Error when processing message", e);
            
            // Create a fallback response in case of error
            ChatItem errorChatItem = new ChatItem();
            errorChatItem.setChatId(request.getChatId());
            errorChatItem.setUserId(request.getUserId());
            errorChatItem.setTeamId(request.getTeamId());
            errorChatItem.setTmbId(request.getTmbId());
            errorChatItem.setAppId(request.getAppId());
            errorChatItem.setTime(LocalDateTime.now());
            errorChatItem.setObj("assistant");
            errorChatItem.setValue("I'm sorry, I encountered an error while processing your request. Please try again later.");
            ChatItem savedErrorChatItem = chatItemRepository.save(errorChatItem);
            
            return chatItemMapper.toDTO(savedErrorChatItem);
        }
    }

    @Override
    public List<ChatItemDTO> getChatMessages(String chatId) {
        if (!chatRepository.findByChatId(chatId).isPresent()) {
            throw new ResourceNotFoundException("Chat", "chatId", chatId);
        }
        List<ChatItem> chatItems = chatItemRepository.findByChatId(chatId);
        return chatItemMapper.toDTOList(chatItems);
    }
    
    /**
     * Process message with workflow
     */
    private ChatItemDTO processWithWorkflow(
            ChatMessageRequest request, 
            Chat chat, 
            App app, 
            ChatItem userChatItem) {
        
        // 检索聊天历史以提供上下文
        List<ChatItem> chatHistory = chatItemRepository.findByChatId(request.getChatId());
        
        // 准备工作流输入
        Map<String, Object> workflowInputs = new HashMap<>();
        
        // 添加基本信息
        workflowInputs.put("user_message", request.getMessage());
        workflowInputs.put("user_id", request.getUserId());
        workflowInputs.put("chat_id", request.getChatId());
        workflowInputs.put("app_id", request.getAppId());
        
        // 添加聊天历史
        List<Map<String, Object>> chatContextList = new ArrayList<>();
        chatHistory.stream()
                .filter(item -> !"system".equals(item.getObj()))
                .sorted(Comparator.comparing(ChatItem::getTime))
                .forEach(item -> {
                    Map<String, Object> messageContext = new HashMap<>();
                    messageContext.put("role", item.getObj());
                    messageContext.put("content", item.getValue());
                    
                    // 如果有元数据，也添加到上下文中
                    if (item.getMetadata() != null && !item.getMetadata().isEmpty()) {
                        messageContext.put("metadata", item.getMetadata());
                    }
                    
                    chatContextList.add(messageContext);
                });
        workflowInputs.put("chat_history", chatContextList);
        
        // 如果有系统提示，添加到上下文
        Optional<String> systemPrompt = chatHistory.stream()
                .filter(item -> "system".equals(item.getObj()))
                .map(ChatItem::getValue)
                .findFirst();
        
        if (systemPrompt.isPresent()) {
            workflowInputs.put("system_prompt", systemPrompt.get());
        } else if (StringUtils.hasText(app.getSystemPrompt())) {
            workflowInputs.put("system_prompt", app.getSystemPrompt());
        }
        
        // 添加App变量
        if (app.getVariables() != null) {
            workflowInputs.put("app_variables", app.getVariables());
        }
        
        // 添加用户对话请求中的元数据
        if (request.getMetadata() != null) {
            workflowInputs.put("user_metadata", request.getMetadata());
        }
        
        log.debug("Executing workflow {} for chat {}", app.getWorkflowId(), request.getChatId());
        
        long startTime = System.currentTimeMillis();
        
        // 执行工作流
        Map<String, Object> workflowResult;
        try {
            workflowResult = workflowService.executeWorkflow(app.getWorkflowId(), workflowInputs);
            log.debug("Workflow {} execution completed in {}ms", 
                    app.getWorkflowId(), (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            log.error("Error executing workflow {}: {}", app.getWorkflowId(), e.getMessage(), e);
            
            // 创建错误响应
            ChatItem errorChatItem = new ChatItem();
            errorChatItem.setChatId(request.getChatId());
            errorChatItem.setUserId(request.getUserId());
            errorChatItem.setTeamId(request.getTeamId());
            errorChatItem.setTmbId(request.getTmbId());
            errorChatItem.setAppId(request.getAppId());
            errorChatItem.setTime(LocalDateTime.now());
            errorChatItem.setObj("assistant");
            errorChatItem.setValue("I'm sorry, I encountered an error while processing your request with workflow. Error: " + e.getMessage());
            
            // 添加错误元数据
            Map<String, Object> errorMetadata = new HashMap<>();
            errorMetadata.put("error", true);
            errorMetadata.put("workflow_id", app.getWorkflowId());
            errorMetadata.put("error_message", e.getMessage());
            errorChatItem.setMetadata(errorMetadata);
            
            ChatItem savedErrorChatItem = chatItemRepository.save(errorChatItem);
            return chatItemMapper.toDTO(savedErrorChatItem);
        }
        
        // 处理工作流输出
        String aiResponse = extractWorkflowResponse(workflowResult);
        
        // 保存AI响应
        ChatItem aiChatItem = new ChatItem();
        aiChatItem.setChatId(request.getChatId());
        aiChatItem.setUserId(request.getUserId());
        aiChatItem.setTeamId(request.getTeamId());
        aiChatItem.setTmbId(request.getTmbId());
        aiChatItem.setAppId(request.getAppId());
        aiChatItem.setTime(LocalDateTime.now());
        aiChatItem.setObj("assistant");
        aiChatItem.setValue(aiResponse);
        
        // 保存工作流元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("workflow_id", app.getWorkflowId());
        metadata.put("execution_time_ms", (System.currentTimeMillis() - startTime));
        
        // 过滤掉大型输入数据，只保留结果和必要的元数据
        Map<String, Object> filteredWorkflowResult = new HashMap<>();
        for (Map.Entry<String, Object> entry : workflowResult.entrySet()) {
            String key = entry.getKey();
            // 忽略大型对象和历史数据
            if (!key.equals("chat_history") && !key.equals("user_message") && !key.contains("__")) {
                filteredWorkflowResult.put(key, entry.getValue());
            }
        }
        metadata.put("workflow_result", filteredWorkflowResult);
        
        // 如果工作流结果包含来源数据，也添加到元数据
        if (workflowResult.containsKey("sources")) {
            metadata.put("sources", workflowResult.get("sources"));
        }
        
        aiChatItem.setMetadata(metadata);
        
        ChatItem savedAiChatItem = chatItemRepository.save(aiChatItem);
        
        return chatItemMapper.toDTO(savedAiChatItem);
    }
    
    /**
     * 从工作流结果中提取响应文本
     */
    private String extractWorkflowResponse(Map<String, Object> workflowResult) {
        // 尝试多种可能的输出字段名
        String[] possibleOutputKeys = {"response", "output", "answer", "ai_response", "text", "result"};
        
        for (String key : possibleOutputKeys) {
            if (workflowResult.containsKey(key) && workflowResult.get(key) != null) {
                return workflowResult.get(key).toString();
            }
        }
        
        // 尝试查找以"output_"开头的字段
        for (Map.Entry<String, Object> entry : workflowResult.entrySet()) {
            if (entry.getKey().startsWith("output_") && entry.getValue() != null) {
                return entry.getValue().toString();
            }
        }
        
        // 如果没有找到任何响应，返回默认消息
        return "I processed your request, but couldn't generate a meaningful response.";
    }
    
    /**
     * Process message with RAG using request-specified knowledge bases
     */
    private ChatItemDTO processWithRag(
            ChatMessageRequest request, 
            Chat chat,
            Optional<App> appOptional,
            ChatItem userChatItem) {
        
        long startTime = System.currentTimeMillis();
        
        // 验证必要的参数
        if (request.getKbIds() == null || request.getKbIds().isEmpty()) {
            log.error("No knowledge base IDs provided for RAG query");
            
            ChatItem errorChatItem = new ChatItem();
            errorChatItem.setChatId(request.getChatId());
            errorChatItem.setUserId(request.getUserId());
            errorChatItem.setTeamId(request.getTeamId());
            errorChatItem.setTmbId(request.getTmbId());
            errorChatItem.setAppId(request.getAppId());
            errorChatItem.setTime(LocalDateTime.now());
            errorChatItem.setObj("assistant");
            errorChatItem.setValue("I'm sorry, I couldn't process your query. No knowledge bases were specified.");
            
            ChatItem savedErrorChatItem = chatItemRepository.save(errorChatItem);
            return chatItemMapper.toDTO(savedErrorChatItem);
        }
        
        // 准备额外参数
        Map<String, Object> extraParams = new HashMap<>();
        
        // 设置限制和分数阈值
        int limit = request.getLimit() != null ? request.getLimit() : 5;
        double minScore = request.getMinScore() != null ? request.getMinScore() : 0.7;
        extraParams.put("limit", limit);
        extraParams.put("minScore", minScore);
        
        // 添加系统提示
        List<ChatItem> chatHistory = chatItemRepository.findByChatId(request.getChatId());
        String systemPrompt = getSystemPrompt(chatHistory, appOptional);
        
        if (StringUtils.hasText(systemPrompt)) {
            extraParams.put("systemPrompt", systemPrompt);
            log.debug("Added system prompt to RAG query: length={}", systemPrompt.length());
        }
        
        // 添加聊天上下文
        List<Map<String, Object>> chatContext = new ArrayList<>();
        chatHistory.stream()
                .filter(item -> !"system".equals(item.getObj()))
                .sorted(Comparator.comparing(ChatItem::getTime))
                .limit(10) // 限制历史消息数量，避免超过最大token限制
                .forEach(item -> {
                    Map<String, Object> messageContext = new HashMap<>();
                    messageContext.put("role", item.getObj());
                    messageContext.put("content", item.getValue());
                    chatContext.add(messageContext);
                });
        
        if (!chatContext.isEmpty()) {
            extraParams.put("chatContext", chatContext);
            log.debug("Added {} chat context messages to RAG query", chatContext.size());
        }
        
        // 添加应用ID和相关配置
        if (StringUtils.hasText(request.getAppId())) {
            extraParams.put("appId", request.getAppId());
            
            // 如果有应用配置，添加到额外参数
            if (appOptional.isPresent()) {
                App app = appOptional.get();
                
                // 添加应用模型配置
                Map<String, Object> modelConfig = chatConfigService.getModelConfig(app.getAppId());
                if (!modelConfig.isEmpty()) {
                    extraParams.put("modelConfig", modelConfig);
                }
                
                // 添加应用元数据
                if (app.getMetadata() != null && !app.getMetadata().isEmpty()) {
                    extraParams.put("appMetadata", app.getMetadata());
                }
            }
        }
        
        // 添加用户请求元数据
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            extraParams.put("requestMetadata", request.getMetadata());
        }
        
        log.debug("Executing RAG query for chat {} using {} knowledge bases", 
                request.getChatId(), request.getKbIds().size());
        
        // 调用RAG服务
        Map<String, Object> ragResult;
        try {
            ragResult = ragService.getRagResponse(
                    request.getMessage(), 
                    request.getKbIds(), 
                    extraParams);
            
            log.debug("RAG query completed in {}ms", (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            log.error("Error during RAG query: {}", e.getMessage(), e);
            
            // 创建错误响应
            ChatItem errorChatItem = new ChatItem();
            errorChatItem.setChatId(request.getChatId());
            errorChatItem.setUserId(request.getUserId());
            errorChatItem.setTeamId(request.getTeamId());
            errorChatItem.setTmbId(request.getTmbId());
            errorChatItem.setAppId(request.getAppId());
            errorChatItem.setTime(LocalDateTime.now());
            errorChatItem.setObj("assistant");
            errorChatItem.setValue("I'm sorry, I encountered an error while searching the knowledge base: " + e.getMessage());
            
            // 添加错误元数据
            Map<String, Object> errorMetadata = new HashMap<>();
            errorMetadata.put("error", true);
            errorMetadata.put("kb_ids", request.getKbIds());
            errorMetadata.put("error_message", e.getMessage());
            errorChatItem.setMetadata(errorMetadata);
            
            ChatItem savedErrorChatItem = chatItemRepository.save(errorChatItem);
            return chatItemMapper.toDTO(savedErrorChatItem);
        }
        
        // 提取AI响应
        String aiResponse = extractRagResponse(ragResult);
        
        // 保存AI响应
        ChatItem aiChatItem = new ChatItem();
        aiChatItem.setChatId(request.getChatId());
        aiChatItem.setUserId(request.getUserId());
        aiChatItem.setTeamId(request.getTeamId());
        aiChatItem.setTmbId(request.getTmbId());
        aiChatItem.setAppId(request.getAppId());
        aiChatItem.setTime(LocalDateTime.now());
        aiChatItem.setObj("assistant");
        aiChatItem.setValue(aiResponse);
        
        // 保存RAG元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rag", true);
        metadata.put("kb_ids", request.getKbIds());
        metadata.put("execution_time_ms", (System.currentTimeMillis() - startTime));
        
        // 存储源文档
        @SuppressWarnings("unchecked")
        List<KbDataDTO> sources = (List<KbDataDTO>) ragResult.get("sources");
        if (sources != null && !sources.isEmpty()) {
            metadata.put("sources", sources);
            metadata.put("source_count", sources.size());
            
            // 添加分数统计
            OptionalDouble avgScore = sources.stream()
                    .mapToDouble(KbDataDTO::getScore)
                    .average();
            if (avgScore.isPresent()) {
                metadata.put("avg_score", avgScore.getAsDouble());
            }
            
            // 添加最高分和最低分
            Optional<Double> maxScore = sources.stream()
                    .map(KbDataDTO::getScore)
                    .max(Double::compare);
            if (maxScore.isPresent()) {
                metadata.put("max_score", maxScore.get());
            }
            
            Optional<Double> lowestScore = sources.stream()
                    .map(KbDataDTO::getScore)
                    .min(Double::compare);
            if (lowestScore.isPresent()) {
                metadata.put("min_score", lowestScore.get());
            }
        }
        
        // 如果ragResult包含其他有用的元数据，也添加到metadata
        if (ragResult.containsKey("model")) {
            metadata.put("model", ragResult.get("model"));
        }
        
        if (ragResult.containsKey("tokens")) {
            metadata.put("tokens", ragResult.get("tokens"));
        }
        
        if (ragResult.containsKey("processingStats")) {
            metadata.put("processingStats", ragResult.get("processingStats"));
        }
        
        aiChatItem.setMetadata(metadata);
        
        ChatItem savedAiChatItem = chatItemRepository.save(aiChatItem);
        
        return chatItemMapper.toDTO(savedAiChatItem);
    }
    
    /**
     * 从RAG结果中提取响应文本
     */
    private String extractRagResponse(Map<String, Object> ragResult) {
        // 尝试多种可能的输出字段名
        String[] possibleOutputKeys = {"answer", "response", "output", "text", "result"};
        
        for (String key : possibleOutputKeys) {
            if (ragResult.containsKey(key) && ragResult.get(key) != null) {
                return ragResult.get(key).toString();
            }
        }
        
        // 如果没有找到任何响应，返回默认消息
        return "I searched the knowledge base but couldn't generate a meaningful response.";
    }
    
    /**
     * Process message with RAG using app-configured knowledge bases
     */
    private ChatItemDTO processWithAppRag(
            ChatMessageRequest request, 
            Chat chat,
            App app,
            ChatItem userChatItem) {
        
        // Prepare additional parameters
        Map<String, Object> extraParams = new HashMap<>();
        
        // Extract RAG config from app if available
        if (app.getRagConfig() != null) {
            if (app.getRagConfig().containsKey("limit")) {
                extraParams.put("limit", app.getRagConfig().get("limit"));
            } else {
                extraParams.put("limit", 5); // Default limit
            }
            
            if (app.getRagConfig().containsKey("minScore")) {
                extraParams.put("minScore", app.getRagConfig().get("minScore"));
            } else {
                extraParams.put("minScore", 0.7); // Default minScore
            }
        } else {
            // Use defaults
            extraParams.put("limit", 5);
            extraParams.put("minScore", 0.7);
        }
        
        // Add system prompt if available
        String systemPrompt = getSystemPrompt(chatItemRepository.findByChatId(request.getChatId()), Optional.of(app));
        if (StringUtils.hasText(systemPrompt)) {
            extraParams.put("systemPrompt", systemPrompt);
        }
        
        // Add app ID
        extraParams.put("appId", app.getAppId());
        
        // Call RAG service with app's knowledge bases
        Map<String, Object> ragResult = ragService.getRagResponse(
                request.getMessage(), 
                app.getKbIds(), 
                extraParams);
        
        // Extract AI response
        String aiResponse = ragResult.containsKey("answer") ? 
                ragResult.get("answer").toString() : 
                "I couldn't generate a response.";
        
        // Save AI response
        ChatItem aiChatItem = new ChatItem();
        aiChatItem.setChatId(request.getChatId());
        aiChatItem.setUserId(request.getUserId());
        aiChatItem.setTeamId(request.getTeamId());
        aiChatItem.setTmbId(request.getTmbId());
        aiChatItem.setAppId(request.getAppId());
        aiChatItem.setTime(LocalDateTime.now());
        aiChatItem.setObj("assistant");
        aiChatItem.setValue(aiResponse);
        
        // Store RAG metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rag", true);
        metadata.put("kb_ids", app.getKbIds());
        
        // Store sources if available
        @SuppressWarnings("unchecked")
        List<KbDataDTO> sources = (List<KbDataDTO>) ragResult.get("sources");
        if (sources != null && !sources.isEmpty()) {
            metadata.put("sources", sources);
        }
        
        aiChatItem.setMetadata(metadata);
        
        ChatItem savedAiChatItem = chatItemRepository.save(aiChatItem);
        
        return chatItemMapper.toDTO(savedAiChatItem);
    }
    
    /**
     * Process message with standard AI
     */
    private ChatItemDTO processWithAI(
            ChatMessageRequest request, 
            Chat chat, 
            Optional<App> appOptional) {
        
        // Retrieve chat history for context
        List<ChatItem> chatHistory = chatItemRepository.findByChatId(request.getChatId());
        
        // Prepare messages for the AI
        List<Map<String, String>> messages = prepareMessagesFromHistory(chatHistory);
        
        // Get model config
        Map<String, Object> modelConfig = appOptional.isPresent() ? 
                chatConfigService.getModelConfig(appOptional.get().getAppId()) : 
                Collections.emptyMap();
        
        // Get system prompt
        String systemPrompt = getSystemPrompt(chatHistory, appOptional);
        
        // Call AI service
        String aiResponse = aiService.generateResponse(messages, systemPrompt, modelConfig);
        
        // Save AI response
        ChatItem aiChatItem = new ChatItem();
        aiChatItem.setChatId(request.getChatId());
        aiChatItem.setUserId(request.getUserId());
        aiChatItem.setTeamId(request.getTeamId());
        aiChatItem.setTmbId(request.getTmbId());
        aiChatItem.setAppId(request.getAppId());
        aiChatItem.setTime(LocalDateTime.now());
        aiChatItem.setObj("assistant");
        aiChatItem.setValue(aiResponse);
        ChatItem savedAiChatItem = chatItemRepository.save(aiChatItem);
        
        return chatItemMapper.toDTO(savedAiChatItem);
    }
    
    /**
     * Extract system prompt from chat history or app config
     */
    private String getSystemPrompt(List<ChatItem> chatHistory, Optional<App> app) {
        // First check if there's a system message in chat history
        Optional<String> systemPromptFromHistory = chatHistory.stream()
                .filter(item -> "system".equals(item.getObj()))
                .map(ChatItem::getValue)
                .findFirst();
                
        if (systemPromptFromHistory.isPresent()) {
            return systemPromptFromHistory.get();
        }
        
        // Otherwise, use app's system prompt if available
        if (app.isPresent() && StringUtils.hasText(app.get().getSystemPrompt())) {
            return app.get().getSystemPrompt();
        }
        
        // Fallback to default
        return "You are a helpful AI assistant.";
    }
    
    /**
     * Prepare messages from chat history for AI request
     */
    private List<Map<String, String>> prepareMessagesFromHistory(List<ChatItem> chatHistory) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // Add chat history (excluding system messages)
        // Only include the most recent messages to stay within token limits
        List<ChatItem> recentHistory = chatHistory.stream()
                .filter(item -> !"system".equals(item.getObj()))
                .sorted(Comparator.comparing(ChatItem::getTime).reversed()) // Sort by time descending
                .limit(10) // Take the 10 most recent messages
                .sorted(Comparator.comparing(ChatItem::getTime)) // Sort by time ascending
                .toList();
                
        for (ChatItem item : recentHistory) {
            Map<String, String> message = new HashMap<>();
            if ("user".equals(item.getObj())) {
                message.put("role", "user");
            } else if ("assistant".equals(item.getObj())) {
                message.put("role", "assistant");
            } else {
                continue; // Skip unknown roles
            }
            message.put("content", item.getValue());
            messages.add(message);
        }
        
        return messages;
    }

    @Override
    public void streamChatMessage(ChatMessageRequest request, BiConsumer<String, Boolean> chunkConsumer) {
        // 验证聊天存在
        Chat chat = chatRepository.findByChatId(request.getChatId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat", "chatId", request.getChatId()));
        
        // 更新聊天的最后更新时间
        chat.setUpdateTime(LocalDateTime.now());
        chatRepository.save(chat);
        
        // 保存用户消息
        ChatItem userChatItem = new ChatItem();
        userChatItem.setChatId(request.getChatId());
        userChatItem.setUserId(request.getUserId());
        userChatItem.setTeamId(request.getTeamId());
        userChatItem.setTmbId(request.getTmbId());
        userChatItem.setAppId(request.getAppId());
        userChatItem.setTime(LocalDateTime.now());
        userChatItem.setObj("user");
        userChatItem.setValue(request.getMessage());
        userChatItem.setMetadata(request.getMetadata());
        chatItemRepository.save(userChatItem);
        
        try {
            // 获取App配置
            Optional<App> appOptional = Optional.empty();
            if (StringUtils.hasText(request.getAppId())) {
                appOptional = appRepository.findByAppId(request.getAppId());
            }
            
            // 决定使用哪种处理模式
            if (appOptional.isPresent() && appOptional.get().getUseWorkflow() != null && 
                    appOptional.get().getUseWorkflow() && 
                    StringUtils.hasText(appOptional.get().getWorkflowId())) {
                
                // 流式工作流处理
                streamWithWorkflow(request, chat, appOptional.get(), userChatItem, chunkConsumer);
            } 
            else if (request.getUseRag() != null && request.getUseRag() && 
                    request.getKbIds() != null && !request.getKbIds().isEmpty()) {
                
                // 流式RAG处理
                streamWithRag(request, chat, appOptional, userChatItem, chunkConsumer);
            }
            else if (appOptional.isPresent() && appOptional.get().getUseKb() != null && 
                    appOptional.get().getUseKb() && 
                    appOptional.get().getKbIds() != null && 
                    !appOptional.get().getKbIds().isEmpty()) {
                
                // 流式应用RAG处理
                streamWithAppRag(request, chat, appOptional.get(), userChatItem, chunkConsumer);
            }
            else {
                // 标准AI流式处理
                streamWithAI(request, chat, appOptional, chunkConsumer);
            }
        } catch (Exception e) {
            log.error("Error streaming chat response", e);
            
            // 创建错误响应
            ChatItem errorChatItem = new ChatItem();
            errorChatItem.setChatId(request.getChatId());
            errorChatItem.setUserId(request.getUserId());
            errorChatItem.setTeamId(request.getTeamId());
            errorChatItem.setTmbId(request.getTmbId());
            errorChatItem.setAppId(request.getAppId());
            errorChatItem.setTime(LocalDateTime.now());
            errorChatItem.setObj("assistant");
            errorChatItem.setValue("I'm sorry, I encountered an error while processing your request. Error: " + e.getMessage());
            
            // 添加错误元数据
            Map<String, Object> errorMetadata = new HashMap<>();
            errorMetadata.put("error", true);
            errorMetadata.put("error_message", e.getMessage());
            errorChatItem.setMetadata(errorMetadata);
            
            chatItemRepository.save(errorChatItem);
            
            // 发送错误消息
            chunkConsumer.accept(errorChatItem.getValue(), true);
        }
    }

    /**
     * 使用标准AI流式处理消息
     */
    private void streamWithAI(
            ChatMessageRequest request, 
            Chat chat, 
            Optional<App> appOptional,
            BiConsumer<String, Boolean> chunkConsumer) {
        
        // 检索聊天历史上下文
        List<ChatItem> chatHistory = chatItemRepository.findByChatId(request.getChatId());
        
        // 准备消息
        List<Map<String, String>> messages = prepareMessagesFromHistory(chatHistory);
        
        // 获取模型配置
        Map<String, Object> modelConfig = appOptional.isPresent() ? 
                chatConfigService.getModelConfig(appOptional.get().getAppId()) : 
                Collections.emptyMap();
        
        // 获取系统提示
        String systemPrompt = getSystemPrompt(chatHistory, appOptional);
        
        // 记录AI响应的构建器
        StringBuilder responseBuilder = new StringBuilder();
        
        // 调用AI服务进行流式生成
        aiService.generateStreamingResponse(messages, systemPrompt, modelConfig, (chunk, isLast) -> {
            // 累积响应内容
            responseBuilder.append(chunk);
            
            // 转发chunk到客户端
            chunkConsumer.accept(chunk, isLast);
            
            // 如果是最后一块，保存完整的响应
            if (isLast) {
                ChatItem aiChatItem = new ChatItem();
                aiChatItem.setChatId(request.getChatId());
                aiChatItem.setUserId(request.getUserId());
                aiChatItem.setTeamId(request.getTeamId());
                aiChatItem.setTmbId(request.getTmbId());
                aiChatItem.setAppId(request.getAppId());
                aiChatItem.setTime(LocalDateTime.now());
                aiChatItem.setObj("assistant");
                aiChatItem.setValue(responseBuilder.toString());
                chatItemRepository.save(aiChatItem);
            }
        });
    }

    /**
     * 使用RAG流式处理消息
     */
    private void streamWithRag(
            ChatMessageRequest request, 
            Chat chat,
            Optional<App> appOptional,
            ChatItem userChatItem,
            BiConsumer<String, Boolean> chunkConsumer) {
        
        long startTime = System.currentTimeMillis();
        
        // 验证必要的参数
        if (request.getKbIds() == null || request.getKbIds().isEmpty()) {
            log.error("No knowledge base IDs provided for RAG query");
            chunkConsumer.accept("I'm sorry, I couldn't process your query. No knowledge bases were specified.", true);
            return;
        }
        
        // 准备额外参数
        Map<String, Object> extraParams = new HashMap<>();
        
        // 设置限制和分数阈值
        int limit = request.getLimit() != null ? request.getLimit() : 5;
        double minScore = request.getMinScore() != null ? request.getMinScore() : 0.7;
        extraParams.put("limit", limit);
        extraParams.put("minScore", minScore);
        
        // 添加系统提示
        List<ChatItem> chatHistory = chatItemRepository.findByChatId(request.getChatId());
        String systemPrompt = getSystemPrompt(chatHistory, appOptional);
        
        if (StringUtils.hasText(systemPrompt)) {
            extraParams.put("systemPrompt", systemPrompt);
        }
        
        // 添加聊天上下文
        List<Map<String, Object>> chatContext = new ArrayList<>();
        chatHistory.stream()
                .filter(item -> !"system".equals(item.getObj()))
                .sorted(Comparator.comparing(ChatItem::getTime))
                .limit(10)
                .forEach(item -> {
                    Map<String, Object> messageContext = new HashMap<>();
                    messageContext.put("role", item.getObj());
                    messageContext.put("content", item.getValue());
                    chatContext.add(messageContext);
                });
        
        if (!chatContext.isEmpty()) {
            extraParams.put("chatContext", chatContext);
        }
        
        // 添加应用ID和模型配置
        if (StringUtils.hasText(request.getAppId())) {
            extraParams.put("appId", request.getAppId());
            
            if (appOptional.isPresent()) {
                App app = appOptional.get();
                Map<String, Object> modelConfig = chatConfigService.getModelConfig(app.getAppId());
                if (!modelConfig.isEmpty()) {
                    extraParams.put("modelConfig", modelConfig);
                }
            }
        }
        
        // 请求流式RAG处理
        extraParams.put("streaming", true);
        
        // 响应构建器
        StringBuilder responseBuilder = new StringBuilder();
        
        // 调用RAG服务
        try {
            ragService.streamRagResponse(
                    request.getMessage(), 
                    request.getKbIds(), 
                    extraParams,
                    (chunk, isLast) -> {
                        // 累积响应
                        responseBuilder.append(chunk);
                        
                        // 转发给客户端
                        chunkConsumer.accept(chunk, isLast);
                        
                        // 如果是最后一块，保存完整响应
                        if (isLast) {
                            // 保存AI响应
                            ChatItem aiChatItem = new ChatItem();
                            aiChatItem.setChatId(request.getChatId());
                            aiChatItem.setUserId(request.getUserId());
                            aiChatItem.setTeamId(request.getTeamId());
                            aiChatItem.setTmbId(request.getTmbId());
                            aiChatItem.setAppId(request.getAppId());
                            aiChatItem.setTime(LocalDateTime.now());
                            aiChatItem.setObj("assistant");
                            aiChatItem.setValue(responseBuilder.toString());
                            
                            // 获取RAG结果的元数据
                            Map<String, Object> ragResult = ragService.getRagMetadata(request.getMessage(), request.getKbIds(), extraParams);
                            
                            // 保存RAG元数据
                            Map<String, Object> metadata = new HashMap<>();
                            metadata.put("rag", true);
                            metadata.put("kb_ids", request.getKbIds());
                            metadata.put("execution_time_ms", (System.currentTimeMillis() - startTime));
                            
                            // 存储源文档
                            if (ragResult.containsKey("sources")) {
                                metadata.put("sources", ragResult.get("sources"));
                            }
                            
                            aiChatItem.setMetadata(metadata);
                            chatItemRepository.save(aiChatItem);
                        }
                    });
        } catch (Exception e) {
            log.error("Error during streaming RAG query: {}", e.getMessage(), e);
            chunkConsumer.accept("I'm sorry, I encountered an error while searching the knowledge base: " + e.getMessage(), true);
        }
    }

    /**
     * 使用应用配置的RAG流式处理消息
     */
    private void streamWithAppRag(
            ChatMessageRequest request, 
            Chat chat,
            App app,
            ChatItem userChatItem,
            BiConsumer<String, Boolean> chunkConsumer) {
        
        // 基本上与streamWithRag类似，但使用应用配置的知识库
        Map<String, Object> extraParams = new HashMap<>();
        
        // 提取RAG配置
        if (app.getRagConfig() != null) {
            if (app.getRagConfig().containsKey("limit")) {
                extraParams.put("limit", app.getRagConfig().get("limit"));
            } else {
                extraParams.put("limit", 5);
            }
            
            if (app.getRagConfig().containsKey("minScore")) {
                extraParams.put("minScore", app.getRagConfig().get("minScore"));
            } else {
                extraParams.put("minScore", 0.7);
            }
        } else {
            extraParams.put("limit", 5);
            extraParams.put("minScore", 0.7);
        }
        
        // 添加系统提示
        String systemPrompt = getSystemPrompt(chatItemRepository.findByChatId(request.getChatId()), Optional.of(app));
        if (StringUtils.hasText(systemPrompt)) {
            extraParams.put("systemPrompt", systemPrompt);
        }
        
        // 添加应用ID
        extraParams.put("appId", app.getAppId());
        
        // 请求流式处理
        extraParams.put("streaming", true);
        
        // 响应构建器
        StringBuilder responseBuilder = new StringBuilder();
        
        // 调用RAG服务
        ragService.streamRagResponse(
                request.getMessage(), 
                app.getKbIds(), 
                extraParams,
                (chunk, isLast) -> {
                    // 累积响应
                    responseBuilder.append(chunk);
                    
                    // 转发给客户端
                    chunkConsumer.accept(chunk, isLast);
                    
                    // 如果是最后一块，保存完整响应
                    if (isLast) {
                        // 保存AI响应
                        ChatItem aiChatItem = new ChatItem();
                        aiChatItem.setChatId(request.getChatId());
                        aiChatItem.setUserId(request.getUserId());
                        aiChatItem.setTeamId(request.getTeamId());
                        aiChatItem.setTmbId(request.getTmbId());
                        aiChatItem.setAppId(request.getAppId());
                        aiChatItem.setTime(LocalDateTime.now());
                        aiChatItem.setObj("assistant");
                        aiChatItem.setValue(responseBuilder.toString());
                        
                        // 保存RAG元数据
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("rag", true);
                        metadata.put("kb_ids", app.getKbIds());
                        
                        // 获取源文档
                        Map<String, Object> ragResult = ragService.getRagMetadata(request.getMessage(), app.getKbIds(), extraParams);
                        if (ragResult.containsKey("sources")) {
                            metadata.put("sources", ragResult.get("sources"));
                        }
                        
                        aiChatItem.setMetadata(metadata);
                        chatItemRepository.save(aiChatItem);
                    }
                });
    }

    /**
     * 使用工作流流式处理消息
     */
    private void streamWithWorkflow(
            ChatMessageRequest request, 
            Chat chat,
            App app,
            ChatItem userChatItem,
            BiConsumer<String, Boolean> chunkConsumer) {
        
        // 检索聊天历史以提供上下文
        List<ChatItem> chatHistory = chatItemRepository.findByChatId(request.getChatId());
        
        // 准备工作流输入
        Map<String, Object> workflowInputs = new HashMap<>();
        
        // 添加基本信息
        workflowInputs.put("user_message", request.getMessage());
        workflowInputs.put("user_id", request.getUserId());
        workflowInputs.put("chat_id", request.getChatId());
        workflowInputs.put("app_id", request.getAppId());
        workflowInputs.put("streaming", true);  // 指示应该流式处理
        
        // 添加聊天历史
        List<Map<String, Object>> chatContextList = new ArrayList<>();
        chatHistory.stream()
                .filter(item -> !"system".equals(item.getObj()))
                .sorted(Comparator.comparing(ChatItem::getTime))
                .forEach(item -> {
                    Map<String, Object> messageContext = new HashMap<>();
                    messageContext.put("role", item.getObj());
                    messageContext.put("content", item.getValue());
                    
                    if (item.getMetadata() != null && !item.getMetadata().isEmpty()) {
                        messageContext.put("metadata", item.getMetadata());
                    }
                    
                    chatContextList.add(messageContext);
                });
        workflowInputs.put("chat_history", chatContextList);
        
        // 添加系统提示
        Optional<String> systemPrompt = chatHistory.stream()
                .filter(item -> "system".equals(item.getObj()))
                .map(ChatItem::getValue)
                .findFirst();
        
        if (systemPrompt.isPresent()) {
            workflowInputs.put("system_prompt", systemPrompt.get());
        } else if (StringUtils.hasText(app.getSystemPrompt())) {
            workflowInputs.put("system_prompt", app.getSystemPrompt());
        }
        
        // 添加App变量
        if (app.getVariables() != null) {
            workflowInputs.put("app_variables", app.getVariables());
        }
        
        log.debug("Executing streaming workflow {} for chat {}", app.getWorkflowId(), request.getChatId());
        
        // 响应构建器
        StringBuilder responseBuilder = new StringBuilder();
        
        // 执行流式工作流
        try {
            workflowService.streamWorkflow(
                    app.getWorkflowId(),
                    workflowInputs,
                    (chunk, isLast) -> {
                        // 累积响应
                        responseBuilder.append(chunk);
                        
                        // 转发给客户端
                        chunkConsumer.accept(chunk, isLast);
                        
                        // 如果是最后一块，保存完整响应
                        if (isLast) {
                            // 保存AI响应
                            ChatItem aiChatItem = new ChatItem();
                            aiChatItem.setChatId(request.getChatId());
                            aiChatItem.setUserId(request.getUserId());
                            aiChatItem.setTeamId(request.getTeamId());
                            aiChatItem.setTmbId(request.getTmbId());
                            aiChatItem.setAppId(request.getAppId());
                            aiChatItem.setTime(LocalDateTime.now());
                            aiChatItem.setObj("assistant");
                            aiChatItem.setValue(responseBuilder.toString());
                            
                            // 保存工作流元数据
                            Map<String, Object> metadata = new HashMap<>();
                            metadata.put("workflow_id", app.getWorkflowId());
                            
                            // 尝试获取工作流执行元数据
                            Map<String, Object> workflowMetadata = workflowService.getExecutionMetadata(app.getWorkflowId());
                            if (workflowMetadata != null && !workflowMetadata.isEmpty()) {
                                metadata.put("workflow_metadata", workflowMetadata);
                            }
                            
                            aiChatItem.setMetadata(metadata);
                            chatItemRepository.save(aiChatItem);
                        }
                    });
        } catch (Exception e) {
            log.error("Error executing streaming workflow {}: {}", app.getWorkflowId(), e.getMessage(), e);
            chunkConsumer.accept("I'm sorry, I encountered an error while processing your request: " + e.getMessage(), true);
        }
    }
} 