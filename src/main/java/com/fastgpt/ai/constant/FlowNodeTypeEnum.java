package com.fastgpt.ai.constant;

/**
 * Enumeration of workflow node types
 */
public enum FlowNodeTypeEnum {
    // Core nodes
    WORKFLOW_START("workflowStart"),
    ANSWER_NODE("answerNode"),
    CHAT_NODE("chatNode"),
    DATASET_SEARCH_NODE("datasetSearchNode"),
    DATASET_CONCAT_NODE("datasetConcatNode"),
    
    // Agent nodes
    CLASSIFY_QUESTION("classifyQuestion"),
    CONTENT_EXTRACT("contentExtract"),
    
    // Tool nodes
    HTTP_REQUEST("httpRequest468"),
    VARIABLE_UPDATE("variableUpdate"),
    CODE("code"),
    TEXT_EDITOR("textEditor"),
    IF_ELSE_NODE("ifElseNode"),
    READ_FILES("readFiles"),
    CUSTOM_FEEDBACK("customFeedback"),
    
    // Tool execution
    TOOLS("tools"),
    STOP_TOOL("stopTool"),
    TOOL_PARAMS("toolParams"),
    QUERY_EXTENSION("queryExtension"),
    
    // Plugin system
    APP_MODULE("appModule"),
    PLUGIN_MODULE("pluginModule"),
    PLUGIN_INPUT("pluginInput"),
    PLUGIN_OUTPUT("pluginOutput"),
    
    // Loop control
    LOOP("loop"),
    LOOP_START("loopStart"),
    LOOP_END("loopEnd"),
    
    // Interactive nodes
    USER_SELECT("userSelect"),
    FORM_INPUT("formInput"),
    
    // System nodes
    SYSTEM_CONFIG("systemConfig"),
    PLUGIN_CONFIG("pluginConfig"),
    EMPTY_NODE("emptyNode"),
    GLOBAL_VARIABLE("globalVariable"),
    COMMENT("comment");
    
    private final String value;
    
    FlowNodeTypeEnum(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static FlowNodeTypeEnum fromValue(String value) {
        for (FlowNodeTypeEnum type : FlowNodeTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + value);
    }
} 