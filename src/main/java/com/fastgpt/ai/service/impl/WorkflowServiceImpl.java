package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.ConnectionDTO;
import com.fastgpt.ai.dto.request.WorkflowCreateRequest;
import com.fastgpt.ai.dto.request.WorkflowUpdateRequest;
import com.fastgpt.ai.entity.Workflow;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.mapper.WorkflowMapper;
import com.fastgpt.ai.repository.WorkflowRepository;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.KnowledgeBaseService;
import com.fastgpt.ai.service.WorkflowService;
import com.fastgpt.ai.service.WorkflowMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowMapper workflowMapper;
    private final AiService aiService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final WorkflowMonitorService monitorService;
    
    // Map of node type to node handler functions
    private final Map<String, Function<NodeExecutionContext, NodeOutDTO>> nodeHandlers = Map.of(
        "ai", this::handleAiNode,
        "function", this::handleFunctionNode,
        "kb", this::handleKbNode,
        "input", this::handleInputNode,
        "output", this::handleOutputNode
    );

    @Override
    @Transactional
    public WorkflowDTO createWorkflow(WorkflowCreateRequest request) {
        Workflow workflow = workflowMapper.toEntity(request);
        
        // Generate a unique workflowId
        workflow.setWorkflowId(UUID.randomUUID().toString());
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        workflow.setCreateTime(now);
        workflow.setUpdateTime(now);
        
        workflow.setStatus("draft");
        
        Workflow savedWorkflow = workflowRepository.save(workflow);
        
        return workflowMapper.toDTO(savedWorkflow);
    }

    @Override
    public WorkflowDTO getWorkflowById(String workflowId) {
        return workflowRepository.findByWorkflowId(workflowId)
                .map(workflowMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "workflowId", workflowId));
    }

    @Override
    public List<WorkflowDTO> getWorkflowsByUserId(String userId) {
        return workflowMapper.toDTOList(workflowRepository.findByUserId(userId));
    }

    @Override
    public List<WorkflowDTO> getWorkflowsByTeamId(String teamId) {
        return workflowMapper.toDTOList(workflowRepository.findByTeamId(teamId));
    }

    @Override
    public List<WorkflowDTO> getWorkflowsByAppId(String appId) {
        return workflowMapper.toDTOList(workflowRepository.findByAppId(appId));
    }

    @Override
    public List<WorkflowDTO> getWorkflowsByModuleId(String moduleId) {
        return workflowMapper.toDTOList(workflowRepository.findByModuleId(moduleId));
    }

    @Override
    public List<WorkflowDTO> getWorkflowTemplates() {
        return workflowMapper.toDTOList(workflowRepository.findByIsTemplate(true));
    }

    @Override
    @Transactional
    public WorkflowDTO updateWorkflow(WorkflowUpdateRequest request) {
        Workflow workflow = workflowRepository.findByWorkflowId(request.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "workflowId", request.getWorkflowId()));
        
        // Update fields if provided
        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        
        if (request.getNodes() != null) {
            workflow.setNodes(workflowMapper.nodeDefsToJson(request.getNodes()));
        }
        
        if (request.getEdges() != null) {
            workflow.setEdges(workflowMapper.connectionsToJson(request.getEdges()));
        }
        
        if (request.getDefaultInputs() != null) {
            workflow.setDefaultInputs(request.getDefaultInputs());
        }
        
        if (request.getConfig() != null) {
            workflow.setConfig(request.getConfig());
        }
        
        if (request.getStatus() != null) {
            workflow.setStatus(request.getStatus());
        }
        
        // Update timestamp
        workflow.setUpdateTime(LocalDateTime.now());
        
        Workflow updatedWorkflow = workflowRepository.save(workflow);
        
        return workflowMapper.toDTO(updatedWorkflow);
    }

    @Override
    @Transactional
    public void deleteWorkflow(String workflowId) {
        if (!workflowRepository.findByWorkflowId(workflowId).isPresent()) {
            throw new ResourceNotFoundException("Workflow", "workflowId", workflowId);
        }
        
        workflowRepository.deleteByWorkflowId(workflowId);
    }

    @Override
    public Map<String, Object> executeWorkflow(String workflowId, Map<String, Object> inputs) {
        WorkflowDTO workflow = getWorkflowById(workflowId);
        return dispatchWorkflow(workflow, inputs, null);
    }

    @Override
    public Map<String, Object> dispatchWorkflow(WorkflowDTO workflow, Map<String, Object> inputs, String startNodeId) {
        // 初始化执行
        long startTime = System.currentTimeMillis();
        String workflowId = workflow.getWorkflowId();
        
        // 对输入参数进行验证和清理
        Map<String, Object> sanitizedInputs = sanitizeInputs(inputs);
        
        // 记录工作流开始
        String executionId = monitorService.recordWorkflowStart(workflowId, sanitizedInputs);
        log.info("Starting workflow execution: {} (ID: {})", workflowId, executionId);
        
        // 初始化工作流执行上下文
        Map<String, Object> executionContext = new HashMap<>();
        executionContext.put("__execution_id", executionId);
        executionContext.put("__workflow_id", workflowId);
        executionContext.put("__start_time", startTime);
        
        // 添加输入参数到上下文
        if (sanitizedInputs != null) {
            executionContext.putAll(sanitizedInputs);
            log.debug("Added {} input parameters to execution context", sanitizedInputs.size());
        }
        
        // 添加默认输入值（如果有且未被输入参数覆盖）
        int defaultsAdded = 0;
        if (workflow.getDefaultInputs() != null) {
            for (Map.Entry<String, Object> entry : workflow.getDefaultInputs().entrySet()) {
                if (entry.getKey() != null && !executionContext.containsKey(entry.getKey())) {
                    executionContext.put(entry.getKey(), entry.getValue());
                    defaultsAdded++;
                }
            }
            if (defaultsAdded > 0) {
                log.debug("Added {} default input values to execution context", defaultsAdded);
            }
        }
        
        // 验证工作流结构
        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            String error = "Workflow has no nodes";
            monitorService.recordWorkflowError(executionId, workflowId, error, 
                    System.currentTimeMillis() - startTime, 0);
            throw new WorkflowExecutionException(error);
        }
        
        if (workflow.getEdges() == null || workflow.getEdges().isEmpty()) {
            log.warn("Workflow {} has no edges defined, execution may be limited", workflowId);
        }
        
        // 创建节点和连接映射，用于快速查找
        Map<String, NodeDefDTO> nodeMap;
        try {
            nodeMap = workflow.getNodes().stream()
                    .filter(n -> n != null && n.getId() != null)
                    .collect(Collectors.toMap(
                            NodeDefDTO::getId,
                            Function.identity(),
                            (a, b) -> {
                                log.warn("Duplicate node ID found: {}, using first definition", a.getId());
                                return a;
                            }
                    ));
            
            if (nodeMap.size() < workflow.getNodes().size()) {
                log.warn("Some nodes were skipped due to missing IDs or duplicate IDs");
            }
        } catch (Exception e) {
            String error = "Failed to process workflow nodes: " + e.getMessage();
            log.error(error, e);
            monitorService.recordWorkflowError(executionId, workflowId, error, 
                    System.currentTimeMillis() - startTime, 0);
            throw new WorkflowExecutionException(error, e);
        }
        
        // 创建目标节点连接映射（目标节点ID -> 进入该节点的连接列表）
        Map<String, List<ConnectionDTO>> targetConnectionMap;
        // 创建源节点连接映射（源节点ID -> 离开该节点的连接列表）
        Map<String, List<ConnectionDTO>> sourceConnectionMap;
        
        try {
            if (workflow.getEdges() == null) {
                targetConnectionMap = Collections.emptyMap();
                sourceConnectionMap = Collections.emptyMap();
            } else {
                targetConnectionMap = workflow.getEdges().stream()
                        .filter(e -> e != null && e.getTargetNodeId() != null)
                        .collect(Collectors.groupingBy(ConnectionDTO::getTargetNodeId));
                
                sourceConnectionMap = workflow.getEdges().stream()
                        .filter(e -> e != null && e.getSourceNodeId() != null)
                        .collect(Collectors.groupingBy(ConnectionDTO::getSourceNodeId));
            }
            
            log.debug("Processed {} nodes and {} edges in workflow {}", 
                    nodeMap.size(), 
                    workflow.getEdges() != null ? workflow.getEdges().size() : 0, 
                    workflowId);
        } catch (Exception e) {
            String error = "Failed to process workflow connections: " + e.getMessage();
            log.error(error, e);
            monitorService.recordWorkflowError(executionId, workflowId, error, 
                    System.currentTimeMillis() - startTime, 0);
            throw new WorkflowExecutionException(error, e);
        }
        
        // 跟踪已访问节点，防止循环
        Set<String> visitedNodes = new HashSet<>();
        
        // 跟踪执行输出
        Map<String, Object> outputs = new HashMap<>();
        
        try {
            // 如果没有指定起始节点，查找并执行入口节点
            if (startNodeId == null || startNodeId.isEmpty()) {
                List<NodeDefDTO> entryNodes = workflow.getNodes().stream()
                        .filter(node -> Boolean.TRUE.equals(node.getIsEntry()))
                        .collect(Collectors.toList());
                
                if (entryNodes.isEmpty()) {
                    String error = "No entry nodes found in workflow";
                    monitorService.recordWorkflowError(executionId, workflowId, error, 
                            System.currentTimeMillis() - startTime, 0);
                    throw new WorkflowExecutionException(error);
                }
                
                log.debug("Found {} entry nodes to execute in workflow {}", entryNodes.size(), workflowId);
                
                // 执行每个入口节点及其下游路径
                for (NodeDefDTO entryNode : entryNodes) {
                    log.debug("Executing entry node: {} ({})", entryNode.getName(), entryNode.getId());
                    
                    Map<String, Object> nodeResults = executeNodeAndDownstream(
                            entryNode.getId(),
                            nodeMap,
                            targetConnectionMap,
                            sourceConnectionMap,
                            executionContext,
                            visitedNodes,
                            workflow);
                    
                    if (nodeResults != null && !nodeResults.isEmpty()) {
                        outputs.putAll(nodeResults);
                        log.debug("Added {} outputs from entry node {}", nodeResults.size(), entryNode.getId());
                    }
                }
            } else {
                // 验证并执行指定的起始节点
                if (!nodeMap.containsKey(startNodeId)) {
                    String error = "Start node not found: " + startNodeId;
                    monitorService.recordWorkflowError(executionId, workflowId, error, 
                            System.currentTimeMillis() - startTime, 0);
                    throw new WorkflowExecutionException(error);
                }
                
                NodeDefDTO startNode = nodeMap.get(startNodeId);
                log.debug("Executing specified start node: {} ({})", startNode.getName(), startNode.getId());
                
                Map<String, Object> nodeResults = executeNodeAndDownstream(
                        startNodeId,
                        nodeMap,
                        targetConnectionMap,
                        sourceConnectionMap,
                        executionContext,
                        visitedNodes,
                        workflow);
                
                if (nodeResults != null && !nodeResults.isEmpty()) {
                    outputs.putAll(nodeResults);
                    log.debug("Added {} outputs from start node {}", nodeResults.size(), startNodeId);
                }
            }
            
            // 删除内部变量，不暴露给调用者
            outputs.remove("__execution_id");
            outputs.remove("__workflow_id");
            outputs.remove("__start_time");
            
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;
            
            // 记录工作流完成
            monitorService.recordWorkflowComplete(executionId, workflowId, outputs, durationMs, visitedNodes.size());
            
            log.info("Workflow execution completed: {} (ID: {}) in {}ms, processed {} nodes", 
                    workflowId, executionId, durationMs, visitedNodes.size());
            
            return outputs;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;
            
            // 添加发生错误时的诊断信息
            Map<String, Object> diagnostics = new HashMap<>();
            diagnostics.put("workflowId", workflowId);
            diagnostics.put("executionId", executionId);
            diagnostics.put("nodesProcessed", visitedNodes.size());
            diagnostics.put("executionPath", new ArrayList<>(visitedNodes));
            diagnostics.put("durationMs", durationMs);
            
            // 记录工作流错误
            monitorService.recordWorkflowError(executionId, workflowId, e.getMessage(), 
                    durationMs, visitedNodes.size());
            
            String errorMsg = String.format("Error executing workflow %s (ID: %s): %s", 
                    workflowId, executionId, e.getMessage());
            log.error(errorMsg, e);
            
            // 将诊断信息附加到异常
            WorkflowExecutionException workflowError = new WorkflowExecutionException(
                    "Workflow execution failed: " + e.getMessage(), e);
            // 在实际实现中，可以将diagnostics添加到异常的自定义字段
            
            throw workflowError;
        }
    }

    /**
     * Execute a node and all its downstream nodes recursively
     */
    private Map<String, Object> executeNodeAndDownstream(
            String nodeId,
            Map<String, NodeDefDTO> nodeMap,
            Map<String, List<ConnectionDTO>> targetConnectionMap,
            Map<String, List<ConnectionDTO>> sourceConnectionMap,
            Map<String, Object> executionContext,
            Set<String> visitedNodes,
            WorkflowDTO workflow) {
        
        String executionId = (String) executionContext.getOrDefault("__execution_id", "unknown");
        String workflowId = workflow.getWorkflowId();
        
        // 检查递归深度
        int currentDepth = visitedNodes.size();
        if (currentDepth > 100) {
            String error = "Maximum execution depth (100) exceeded - possible infinite loop detected";
            log.warn(error + " at node: {}", nodeId);
            monitorService.recordWorkflowError(executionId, workflowId, error, 
                    System.currentTimeMillis() - (long) executionContext.getOrDefault("__start_time", System.currentTimeMillis()), 
                    visitedNodes.size());
            throw new WorkflowExecutionException(error);
        }
        
        // 检查循环
        if (visitedNodes.contains(nodeId)) {
            log.warn("Cycle detected in workflow at node: {}", nodeId);
            return Collections.emptyMap();
        }
        
        // 标记节点为已访问
        visitedNodes.add(nodeId);
        
        // 获取节点定义
        NodeDefDTO node = nodeMap.get(nodeId);
        if (node == null) {
            String error = "Node not found: " + nodeId;
            log.error(error);
            return Collections.emptyMap();
        }
        
        log.debug("Executing node: {} ({} - Type: {})", node.getName(), nodeId, node.getType());
        
        // 准备节点输入数据
        Map<String, Object> nodeInputs = new HashMap<>();
        nodeInputs.put("__execution_id", executionId);
        nodeInputs.put("__workflow_id", workflowId);
        
        // 获取以该节点为目标的连接
        List<ConnectionDTO> incomingConnections = targetConnectionMap.getOrDefault(nodeId, Collections.emptyList());
        log.debug("Node {} has {} incoming connections", nodeId, incomingConnections.size());
        
        // 对于每个传入连接，从源节点获取输出
        for (ConnectionDTO connection : incomingConnections) {
            String sourceNodeId = connection.getSourceNodeId();
            String sourceHandle = connection.getSourceHandle();
            String targetHandle = connection.getTargetHandle();
            
            if (!StringUtils.hasText(sourceNodeId) || !StringUtils.hasText(sourceHandle) || !StringUtils.hasText(targetHandle)) {
                log.warn("Invalid connection for node {}: sourceNodeId={}, sourceHandle={}, targetHandle={}",
                        nodeId, sourceNodeId, sourceHandle, targetHandle);
                continue;
            }
            
            // 使用源节点和句柄从执行上下文中获取值
            String contextKey = sourceNodeId + "." + sourceHandle;
            Object value = executionContext.get(contextKey);
            
            if (value != null) {
                nodeInputs.put(targetHandle, value);
                log.debug("Added input '{}' from node {} to node {}", targetHandle, sourceNodeId, nodeId);
            } else {
                log.debug("No value found for key '{}', source node may not have been executed yet", contextKey);
            }
        }
        
        // 添加匹配预期输入名称的全局执行上下文值
        if (node.getInputs() != null) {
            for (ConnectionDTO input : node.getInputs()) {
                String inputName = input.getTargetHandle();
                if (executionContext.containsKey(inputName) && !nodeInputs.containsKey(inputName)) {
                    nodeInputs.put(inputName, executionContext.get(inputName));
                    log.debug("Added global input '{}' to node {}", inputName, nodeId);
                }
            }
        }
        
        // 执行节点
        long nodeStartTime = System.currentTimeMillis();
        NodeOutDTO nodeResult;
        
        try {
            nodeResult = executeNode(workflow, nodeId, nodeInputs);
            
            long nodeExecutionTime = System.currentTimeMillis() - nodeStartTime;
            log.debug("Node {} executed in {}ms with status {}", 
                    nodeId, nodeExecutionTime, nodeResult.getStatus());
            
            if (nodeResult.getStatus() != 0) {
                String errorMsg = String.format("Error executing node %s (%s): %s", 
                        node.getName(), nodeId, nodeResult.getMessage());
                log.error(errorMsg);
                throw new WorkflowExecutionException(errorMsg);
            }
        } catch (Exception e) {
            if (!(e instanceof WorkflowExecutionException)) {
                log.error("Unexpected error executing node {}: {}", nodeId, e.getMessage(), e);
            }
            throw e;
        }
        
        // 将节点输出存储在执行上下文中
        Map<String, Object> nodeOutputs = nodeResult.getOutputs();
        if (nodeOutputs != null) {
            for (Map.Entry<String, Object> entry : nodeOutputs.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    String contextKey = nodeId + "." + entry.getKey();
                    executionContext.put(contextKey, entry.getValue());
                    
                    // 同时存储简单的键值对以便全局访问
                    // 注意：这可能会覆盖同名变量
                    executionContext.put(entry.getKey(), entry.getValue());
                }
            }
            log.debug("Added {} output values from node {} to context", nodeOutputs.size(), nodeId);
        }
        
        // 结果映射，累积来自该节点及其下游节点的输出
        Map<String, Object> results = new HashMap<>();
        
        // 如果这是一个输出节点，或者标记为必需节点，将其输出添加到最终结果
        if ("output".equals(node.getType()) || Boolean.TRUE.equals(node.getIsRequired())) {
            if (nodeOutputs != null) {
                results.putAll(nodeOutputs);
                log.debug("Added outputs from node {} to final results (output or required node)", nodeId);
            }
        }
        
        // 查找下游节点
        List<ConnectionDTO> outgoingConnections = sourceConnectionMap.getOrDefault(nodeId, Collections.emptyList());
        log.debug("Node {} has {} outgoing connections", nodeId, outgoingConnections.size());
        
        // 获取唯一目标节点ID列表
        Set<String> uniqueTargetNodeIds = outgoingConnections.stream()
                .map(ConnectionDTO::getTargetNodeId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        
        log.debug("Node {} has {} unique downstream nodes", nodeId, uniqueTargetNodeIds.size());
        
        // 按顺序处理下游节点（如果有定义的顺序）
        for (String targetNodeId : uniqueTargetNodeIds) {
            NodeDefDTO targetNode = nodeMap.get(targetNodeId);
            
            // 跳过不存在的节点
            if (targetNode == null) {
                log.warn("Target node {} does not exist in workflow definition", targetNodeId);
                continue;
            }
            
            try {
                log.debug("Processing downstream node: {} ({}) from node {}", 
                        targetNode.getName(), targetNodeId, nodeId);
                
                Map<String, Object> downstreamResults = executeNodeAndDownstream(
                        targetNodeId,
                        nodeMap,
                        targetConnectionMap,
                        sourceConnectionMap,
                        executionContext,
                        visitedNodes,
                        workflow);
                
                // 合并下游结果
                results.putAll(downstreamResults);
            } catch (Exception e) {
                // 为异常添加执行路径信息
                String errorMessage = String.format("Error in downstream node %s (%s) from node %s (%s): %s",
                        targetNode.getName(), targetNodeId, node.getName(), nodeId, e.getMessage());
                log.error(errorMessage);
                throw new WorkflowExecutionException(errorMessage, e);
            }
        }
        
        return results;
    }

    @Override
    public NodeOutDTO executeNode(WorkflowDTO workflow, String nodeId, Map<String, Object> inputs) {
        // 获取执行ID和工作流ID
        String executionId = (String) inputs.getOrDefault("__execution_id", "unknown");
        String workflowId = (String) inputs.getOrDefault("__workflow_id", workflow.getWorkflowId());
        
        // 查找节点定义
        NodeDefDTO node = workflow.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Node", "id", nodeId));
        
        // 记录节点执行开始
        monitorService.recordNodeStart(executionId, workflowId, nodeId, 
                node.getName(), node.getType(), inputs);
        
        long startTime = System.currentTimeMillis();
        
        // 创建节点执行上下文
        NodeExecutionContext context = new NodeExecutionContext(
                node,
                inputs,
                workflow
        );
        
        // 获取节点类型的处理器
        Function<NodeExecutionContext, NodeOutDTO> handler = nodeHandlers.get(node.getType());
        
        if (handler == null) {
            NodeOutDTO errorResult = NodeOutDTO.error("Unsupported node type: " + node.getType());
            
            // 记录节点执行完成（失败）
            monitorService.recordNodeComplete(executionId, workflowId, nodeId, 
                    errorResult, System.currentTimeMillis() - startTime);
            
            return errorResult;
        }
        
        try {
            // 执行节点
            NodeOutDTO result = handler.apply(context);
            
            // 记录节点执行完成
            monitorService.recordNodeComplete(executionId, workflowId, nodeId, 
                    result, System.currentTimeMillis() - startTime);
            
            return result;
        } catch (Exception e) {
            log.error("Error executing node {}: {}", nodeId, e.getMessage(), e);
            
            NodeOutDTO errorResult = NodeOutDTO.error("Node execution error: " + e.getMessage());
            
            // 记录节点执行完成（失败）
            monitorService.recordNodeComplete(executionId, workflowId, nodeId, 
                    errorResult, System.currentTimeMillis() - startTime);
            
            return errorResult;
        }
    }
    
    /**
     * Handle AI node execution
     */
    private NodeOutDTO handleAiNode(NodeExecutionContext context) {
        Map<String, Object> data = context.getNode().getData();
        Map<String, Object> inputs = context.getInputs();
        
        // Extract configuration from node data
        String prompt = (String) data.getOrDefault("prompt", "");
        String modelId = (String) data.getOrDefault("modelId", "gpt-3.5-turbo");
        
        // Replace placeholders in prompt with input values
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (prompt.contains(placeholder) && entry.getValue() != null) {
                prompt = prompt.replace(placeholder, entry.getValue().toString());
            }
        }
        
        // Build AI request
        Map<String, Object> modelConfig = new HashMap<>();
        modelConfig.put("model", modelId);
        
        // Extract and apply additional configuration
        if (data.containsKey("temperature")) {
            modelConfig.put("temperature", data.get("temperature"));
        }
        
        if (data.containsKey("maxTokens")) {
            modelConfig.put("max_tokens", data.get("maxTokens"));
        }
        
        // Call AI service
        String response = aiService.generateSimpleResponse(prompt, null);
        
        // Build result
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("response", response);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", modelId);
        metadata.put("promptLength", prompt.length());
        metadata.put("responseLength", response.length());
        
        return NodeOutDTO.success(outputs, metadata);
    }
    
    /**
     * Handle Function node execution
     */
    private NodeOutDTO handleFunctionNode(NodeExecutionContext context) {
        Map<String, Object> data = context.getNode().getData();
        Map<String, Object> inputs = context.getInputs();
        
        // Get function type
        String functionType = (String) data.getOrDefault("functionType", "");
        
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            switch (functionType) {
                case "jsonParse":
                    String jsonString = inputs.containsKey("text") ? inputs.get("text").toString() : "";
                    if (StringUtils.hasText(jsonString)) {
                        try {
                            // Simple implementation - in production, use a proper JSON parser
                            outputs.put("parsed", Map.of("result", "Parsed JSON object"));
                            metadata.put("parsed", true);
                        } catch (Exception e) {
                            return NodeOutDTO.error("Failed to parse JSON: " + e.getMessage());
                        }
                    } else {
                        return NodeOutDTO.error("No text provided for JSON parsing");
                    }
                    break;
                
                case "textProcess":
                    String text = inputs.containsKey("text") ? inputs.get("text").toString() : "";
                    String operation = (String) data.getOrDefault("operation", "none");
                    
                    if (StringUtils.hasText(text)) {
                        switch (operation) {
                            case "uppercase":
                                outputs.put("result", text.toUpperCase());
                                break;
                            case "lowercase":
                                outputs.put("result", text.toLowerCase());
                                break;
                            case "trim":
                                outputs.put("result", text.trim());
                                break;
                            default:
                                outputs.put("result", text);
                        }
                    } else {
                        outputs.put("result", "");
                    }
                    metadata.put("operation", operation);
                    break;
                
                case "mathOperation":
                    Double value1 = inputs.containsKey("value1") ? parseDouble(inputs.get("value1")) : 0.0;
                    Double value2 = inputs.containsKey("value2") ? parseDouble(inputs.get("value2")) : 0.0;
                    String mathOp = (String) data.getOrDefault("operation", "add");
                    
                    double result;
                    switch (mathOp) {
                        case "add":
                            result = value1 + value2;
                            break;
                        case "subtract":
                            result = value1 - value2;
                            break;
                        case "multiply":
                            result = value1 * value2;
                            break;
                        case "divide":
                            if (value2 == 0) {
                                return NodeOutDTO.error("Division by zero");
                            }
                            result = value1 / value2;
                            break;
                        default:
                            return NodeOutDTO.error("Unsupported math operation: " + mathOp);
                    }
                    
                    outputs.put("result", result);
                    metadata.put("operation", mathOp);
                    break;
                
                default:
                    return NodeOutDTO.error("Unsupported function type: " + functionType);
            }
            
            return NodeOutDTO.success(outputs, metadata);
        } catch (Exception e) {
            log.error("Error executing function node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Function execution error: " + e.getMessage());
        }
    }
    
    /**
     * Handle Knowledge Base node execution
     */
    private NodeOutDTO handleKbNode(NodeExecutionContext context) {
        Map<String, Object> data = context.getNode().getData();
        Map<String, Object> inputs = context.getInputs();
        
        // Get query from inputs
        String query = inputs.containsKey("query") ? inputs.get("query").toString() : "";
        if (!StringUtils.hasText(query)) {
            return NodeOutDTO.error("No query provided for knowledge base search");
        }
        
        // Get knowledge base IDs from node data
        @SuppressWarnings("unchecked")
        List<String> kbIds = (List<String>) data.getOrDefault("kbIds", Collections.emptyList());
        if (kbIds.isEmpty()) {
            return NodeOutDTO.error("No knowledge bases selected for search");
        }
        
        // Get search parameters
        int limit = data.containsKey("limit") ? parseInteger(data.get("limit")) : 5;
        double minScore = data.containsKey("minScore") ? parseDouble(data.get("minScore")) : 0.7;
        
        try {
            // Create search request
            Map<String, Object> extraParams = new HashMap<>();
            extraParams.put("limit", limit);
            extraParams.put("minScore", minScore);
            
            // Call RAG service through knowledge base service
            List<com.fastgpt.ai.dto.KbDataDTO> searchResults = knowledgeBaseService.search(
                    new com.fastgpt.ai.dto.request.VectorSearchRequest(kbIds.get(0), query, limit, minScore, null, null, false));
            
            // Convert search results to map for output
            Map<String, Object> resultsMap = new HashMap<>();
            for (int i = 0; i < searchResults.size(); i++) {
                com.fastgpt.ai.dto.KbDataDTO result = searchResults.get(i);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("q", result.getQ());
                resultMap.put("a", result.getA());
                resultMap.put("score", result.getScore());
                
                resultsMap.put("source_" + i, resultMap);
            }
            
            // Build result
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("results", resultsMap);
            outputs.put("context", formatSearchResults(resultsMap));
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("kbIds", kbIds);
            metadata.put("resultsCount", resultsMap.size());
            
            return NodeOutDTO.success(outputs, metadata);
        } catch (Exception e) {
            log.error("Error executing KB node: {}", e.getMessage(), e);
            return NodeOutDTO.error("Knowledge base search error: " + e.getMessage());
        }
    }
    
    /**
     * Handle Input node execution
     */
    private NodeOutDTO handleInputNode(NodeExecutionContext context) {
        Map<String, Object> inputs = context.getInputs();
        
        // Pass through all inputs as outputs
        return NodeOutDTO.success(new HashMap<>(inputs));
    }
    
    /**
     * Handle Output node execution
     */
    private NodeOutDTO handleOutputNode(NodeExecutionContext context) {
        Map<String, Object> inputs = context.getInputs();
        
        // Pass through all inputs as outputs
        return NodeOutDTO.success(new HashMap<>(inputs));
    }
    
    /**
     * Context object for node execution
     */
    private static class NodeExecutionContext {
        private final NodeDefDTO node;
        private final Map<String, Object> inputs;
        private final WorkflowDTO workflow;
        
        public NodeExecutionContext(NodeDefDTO node, Map<String, Object> inputs, WorkflowDTO workflow) {
            this.node = node;
            this.inputs = inputs != null ? inputs : Collections.emptyMap();
            this.workflow = workflow;
        }
        
        public NodeDefDTO getNode() {
            return node;
        }
        
        public Map<String, Object> getInputs() {
            return inputs;
        }
        
        public WorkflowDTO getWorkflow() {
            return workflow;
        }
    }
    
    /**
     * Format search results for context display
     */
    private String formatSearchResults(Map<String, Object> searchResults) {
        StringBuilder builder = new StringBuilder();
        builder.append("Context information:\n\n");
        
        int index = 1;
        for (Map.Entry<String, Object> entry : searchResults.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) entry.getValue();
            
            builder.append("--- Document ").append(index++).append(" ---\n");
            builder.append("Q: ").append(result.get("q")).append("\n");
            
            if (result.containsKey("a") && result.get("a") != null) {
                builder.append("A: ").append(result.get("a")).append("\n");
            }
            
            builder.append("\n");
        }
        
        return builder.toString();
    }
    
    /**
     * Parse double safely
     */
    private Double parseDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Parse integer safely
     */
    private Integer parseInteger(Object value) {
        if (value == null) {
            return 0;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 验证和清理输入参数
     */
    private Map<String, Object> sanitizeInputs(Map<String, Object> inputs) {
        if (inputs == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> sanitized = new HashMap<>();
        
        // 过滤掉空键和系统保留键
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key == null || key.startsWith("__")) {
                continue;
            }
            
            // 防止null值
            if (value != null) {
                sanitized.put(key, value);
            }
        }
        
        return sanitized;
    }

    @Override
    public List<WorkflowDTO> getWorkflowsByUserIdOrTeamId(String userId, String teamId) {
        return workflowMapper.toDTOList(workflowRepository.findByUserIdOrTeamId(userId, teamId));
    }
} 