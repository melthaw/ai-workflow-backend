package com.fastgpt.ai.service.impl.node;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.service.NodeDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码节点调度器
 * 对应Next.js版本的code节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeNodeDispatcher implements NodeDispatcher {

    @Override
    public String getNodeType() {
        return FlowNodeTypeEnum.CODE.toString();
    }

    @Override
    public NodeOutDTO dispatch(Node node, Map<String, Object> inputs) {
        log.info("Processing code node: {}", node.getNodeId());
        
        try {
            // 取得节点配置
            Map<String, Object> data = node.getInputValue("data", new HashMap<>());
            
            // 获取代码和语言类型
            String code = node.getInputValue("code", "");
            String language = node.getInputValue("language", "javascript");
            
            if (code.isEmpty()) {
                throw new IllegalArgumentException("代码内容不能为空");
            }
            
            Map<String, Object> result;
            
            // 根据语言类型执行代码
            switch (language.toLowerCase()) {
                case "javascript":
                    result = executeJavaScript(code, inputs);
                    break;
                case "groovy":
                    result = executeGroovy(code, inputs);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的编程语言: " + language);
            }
            
            // 日志记录
            log.info("Code node {} executed successfully", node.getNodeId());
            
            // 构建输出
            return NodeOutDTO.builder()
                .success(true)
                .nodeId(node.getNodeId())
                .output(result)
                .build();
            
        } catch (Exception e) {
            log.error("Error executing code node: {}", e.getMessage(), e);
            
            return NodeOutDTO.builder()
                .success(false)
                .nodeId(node.getNodeId())
                .error("代码执行失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 执行JavaScript代码
     */
    private Map<String, Object> executeJavaScript(String code, Map<String, Object> inputs) 
            throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        
        if (engine == null) {
            throw new IllegalStateException("JavaScript引擎不可用");
        }
        
        // 创建绑定上下文
        SimpleBindings bindings = new SimpleBindings();
        bindings.putAll(inputs);
        
        // 添加结果对象
        bindings.put("result", new HashMap<String, Object>());
        
        // 执行代码
        engine.eval(code, bindings);
        
        // 获取结果
        Object resultObj = bindings.get("result");
        
        if (resultObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) resultObj;
            return resultMap;
        } else {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("output", resultObj != null ? resultObj.toString() : null);
            return resultMap;
        }
    }
    
    /**
     * 执行Groovy代码
     */
    private Map<String, Object> executeGroovy(String code, Map<String, Object> inputs) 
            throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("groovy");
        
        if (engine == null) {
            throw new IllegalStateException("Groovy引擎不可用");
        }
        
        // 创建绑定上下文
        SimpleBindings bindings = new SimpleBindings();
        bindings.putAll(inputs);
        
        // 添加结果对象
        bindings.put("result", new HashMap<String, Object>());
        
        // 执行代码
        engine.eval(code, bindings);
        
        // 获取结果
        Object resultObj = bindings.get("result");
        
        if (resultObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) resultObj;
            return resultMap;
        } else {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("output", resultObj != null ? resultObj.toString() : null);
            return resultMap;
        }
    }
} 