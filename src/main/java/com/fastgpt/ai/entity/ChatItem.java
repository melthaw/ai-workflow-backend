package com.fastgpt.ai.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a chat message or interaction within a chat.
 */
@Data
@Document(collection = "chat_items")
public class ChatItem {
    
    @Id
    private String id;
    
    /**
     * The ID of the chat this message belongs to
     */
    @Field("chat_id")
    private String chatId;
    
    /**
     * The ID of the user who owns this message
     */
    @Field("user_id")
    private String userId;
    
    /**
     * The ID of the team if applicable
     */
    @Field("team_id")
    private String teamId;
    
    /**
     * Team member ID if applicable
     */
    @Field("tmb_id")
    private String tmbId;
    
    /**
     * The ID of the app this message is associated with
     */
    @Field("app_id")
    private String appId;
    
    /**
     * Optional data ID for reference
     */
    @Field("data_id")
    private String dataId;
    
    /**
     * The timestamp when this message was created
     */
    private LocalDateTime time;
    
    /**
     * The role/object type: 'user', 'assistant', or 'system'
     */
    private String obj;
    
    /**
     * The message content
     */
    private String value;
    
    /**
     * Value items for multi-modal content
     * Can contain text, files, images, etc.
     */
    @Field("value_items")
    private List<ChatItemValue> valueItems;
    
    /**
     * File reference for this message
     */
    @Field("file")
    private ChatFile file;
    
    /**
     * Buffer for Text-to-Speech if available
     */
    @Field("tts_buffer")
    private byte[] ttsBuffer;
    
    /**
     * Feedback provided by the user on this message
     */
    @Field("user_feedback")
    private String userFeedback;
    
    /**
     * Feedback provided by an admin on this message
     */
    @Field("admin_feedback")
    private String adminFeedback;
    
    /**
     * When the user feedback was provided
     */
    @Field("user_feedback_time")
    private LocalDateTime userFeedbackTime;
    
    /**
     * When the admin feedback was provided
     */
    @Field("admin_feedback_time")
    private LocalDateTime adminFeedbackTime;
    
    /**
     * Additional metadata for the message, such as workflow results, RAG sources, etc.
     */
    @Field("metadata")
    private Map<String, Object> metadata;
} 