package com.fastgpt.ai.entity;

import com.fastgpt.ai.entity.config.FileSelectConfig;
import com.fastgpt.ai.entity.config.TTSConfig;
import com.fastgpt.ai.entity.config.WhisperConfig;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "chat")
public class Chat {
    @Id
    private String id;
    
    @Field("chat_id")
    private String chatId;
    
    @Field("user_id")
    private String userId;
    
    @Field("team_id")
    private String teamId;
    
    @Field("tmb_id")
    private String tmbId;
    
    @Field("app_id")
    private String appId;
    
    @Field("update_time")
    private LocalDateTime updateTime;
    
    private String title;
    
    @Field("custom_title")
    private String customTitle;
    
    private Boolean top;
    
    private String source;
    
    @Field("source_name")
    private String sourceName;
    
    @Field("share_id")
    private String shareId;
    
    @Field("out_link_uid")
    private String outLinkUid;
    
    @Field("variable_list")
    private List<Object> variableList;
    
    @Field("welcome_text")
    private String welcomeText;
    
    private Map<String, Object> variables;
    
    @Field("plugin_inputs")
    private List<Object> pluginInputs;
    
    private Map<String, Object> metadata;

    /**
     * Configuration for file selection in chat
     */
    @Field("file_select_config")
    private FileSelectConfig fileSelectConfig;

    /**
     * Configuration for audio processing in chat
     */
    @Field("whisper_config")
    private WhisperConfig whisperConfig;

    /**
     * Configuration for text-to-speech in chat
     */
    @Field("tts_config")
    private TTSConfig ttsConfig;
} 