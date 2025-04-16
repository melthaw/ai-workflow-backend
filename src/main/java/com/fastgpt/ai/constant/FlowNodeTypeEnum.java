package com.fastgpt.ai.constant;

/**
 * 流程节点类型枚举
 * 对应Next.js版本的FlowNodeTypeEnum
 */
public enum FlowNodeTypeEnum {
    // 工作流开始节点
    WORKFLOW_START("workflowStart", "工作流开始"),
    
    // 聊天节点
    CHAT_NODE("chatNode", "聊天节点"),
    
    // 回答节点
    ANSWER_NODE("answerNode", "回答节点"),
    
    // 数据集搜索节点
    DATASET_SEARCH_NODE("datasetSearchNode", "数据集搜索节点"),
    
    // 数据集拼接节点
    DATASET_CONCAT_NODE("datasetConcatNode", "数据集拼接节点"),
    
    // 问题分类节点
    CLASSIFY_QUESTION("classifyQuestion", "问题分类节点"),
    
    // 内容提取节点
    CONTENT_EXTRACT("contentExtract", "内容提取节点"),
    
    // HTTP请求节点
    HTTP_REQUEST_468("httpRequest468", "HTTP请求节点"),
    
    // 应用模块节点
    APP_MODULE("appModule", "应用模块节点"),
    
    // 插件模块节点
    PLUGIN_MODULE("pluginModule", "插件模块节点"),
    
    // 插件输入节点
    PLUGIN_INPUT("pluginInput", "插件输入节点"),
    
    // 插件输出节点
    PLUGIN_OUTPUT("pluginOutput", "插件输出节点"),
    
    // 查询扩展节点
    QUERY_EXTENSION_NODE("queryExtension", "查询扩展节点"),
    
    // 工具节点
    TOOLS("tools", "工具节点"),
    
    // 停止工具节点
    STOP_TOOL("stopTool", "停止工具节点"),
    
    // 工具参数节点
    TOOL_PARAMS("toolParams", "工具参数节点"),
    
    // Laf模块节点
    LAF_MODULE("lafModule", "Laf模块节点"),
    
    // 条件判断节点
    IF_ELSE_NODE("ifElseNode", "条件判断节点"),
    
    // 变量更新节点
    VARIABLE_UPDATE("variableUpdate", "变量更新节点"),
    
    // 代码节点
    CODE("code", "代码节点"),
    
    // 文本编辑器节点
    TEXT_EDITOR("textEditor", "文本编辑器节点"),
    
    // 自定义反馈节点
    CUSTOM_FEEDBACK("customFeedback", "自定义反馈节点"),
    
    // 读取文件节点
    READ_FILES("readFiles", "读取文件节点"),
    
    // 用户选择节点
    USER_SELECT("userSelect", "用户选择节点"),
    
    // 循环节点
    LOOP("loop", "循环节点"),
    
    // 循环开始节点
    LOOP_START("loopStart", "循环开始节点"),
    
    // 循环结束节点
    LOOP_END("loopEnd", "循环结束节点"),
    
    // 表单输入节点
    FORM_INPUT("formInput", "表单输入节点"),
    
    // 系统配置节点
    SYSTEM_CONFIG("systemConfig", "系统配置节点"),
    
    // 插件配置节点
    PLUGIN_CONFIG("pluginConfig", "插件配置节点"),
    
    // 空节点
    EMPTY_NODE("emptyNode", "空节点"),
    
    // 全局变量节点
    GLOBAL_VARIABLE("globalVariable", "全局变量节点"),
    
    // 注释节点
    COMMENT("comment", "注释节点"),
    
    // 运行应用节点（已废弃）
    RUN_APP("runApp", "运行应用节点（已废弃）");
    
    private final String value;
    private final String desc;
    
    FlowNodeTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDesc() {
        return desc;
    }
    
    @Override
    public String toString() {
        return value;
    }
} 