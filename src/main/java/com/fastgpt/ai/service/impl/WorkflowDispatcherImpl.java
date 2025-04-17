package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.EdgeStatusDTO;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.WorkflowDebugResponse;
import com.fastgpt.ai.entity.workflow.Edge;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.entity.workflow.NodeInput;
import com.fastgpt.ai.entity.workflow.NodeOutput;
import com.fastgpt.ai.service.NodeDispatcher;
import com.fastgpt.ai.service.NodeDispatcherRegistry;
import com.fastgpt.ai.service.UsageTrackingService;
import com.fastgpt.ai.service.VariableManager;
import com.fastgpt.ai.service.WorkflowDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 工作流调度器实现
 * 对标Next.js版本的dispatchWorkFlow函数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDispatcherImpl implements WorkflowDispatcher {
    private final NodeDispatcherRegistry nodeDispatcherRegistry;
    private final VariableManager variableManager;
    private final UsageTrackingService usageTrackingService;
    
    // 默认来源
    private static final String DEFAULT_SOURCE = "fastgpt";
    // 最大运行次数
    private static final int MAX_RUN_TIMES = 50;

    @Override
    public Map<String, Object> dispatchWorkflow(WorkflowDTO workflow, 
                                              Map<String, Object> inputs,
                                              String userId,
                                              String teamId,
                                              String appId,
                                              BiConsumer<String, Boolean> streamConsumer) {
        // 保存节点响应和资源使用情况
        List<Map<String, Object>> flowResponses = new ArrayList<>();
        List<Map<String, Object>> flowUsages = new ArrayList<>();
        
        // 初始化变量(包括系统变量和输入变量)
        Map<String, Object> variables = new HashMap<>(inputs);
        variables.putAll(getSystemVariables(userId, teamId, appId));
        
        // 记录执行时间
        long startTime = System.currentTimeMillis();
        
        // 运行时节点和边副本
        List<Node> runtimeNodes = new ArrayList<>(workflow.getNodes());
        List<Edge> runtimeEdges = new ArrayList<>(workflow.getEdges());
        
        // 剩余运行次数
        int remainingRuns = MAX_RUN_TIMES;
        
        // 总输出结果
        Map<String, Object> finalOutputs = new HashMap<>();
        
        try {
            // 找到入口节点并执行
            List<String> nodeIds = executeWorkflow(
                runtimeNodes, 
                runtimeEdges, 
                variables, 
                remainingRuns, 
                flowResponses, 
                flowUsages, 
                streamConsumer, 
                finalOutputs
            );
            
            // 跟踪工作流使用情况
            if (!flowUsages.isEmpty()) {
                usageTrackingService.trackWorkflowUsage(
                    workflow.getName() != null ? workflow.getName() : "未命名工作流",
                    appId, teamId, userId, DEFAULT_SOURCE, flowUsages
                );
            }
            
            return finalOutputs;
        } catch (Exception e) {
            log.error("Error dispatching workflow", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Workflow {} executed in {}ms", workflow.getWorkflowId(), executionTime);
        }
    }

    @Override
    public WorkflowDebugResponse debugWorkflow(WorkflowDTO workflow, 
                                            Map<String, Object> inputs,
                                            String userId,
                                            String teamId,
                                            String appId) {
        // 运行工作流
        Map<String, Object> outputs = dispatchWorkflow(workflow, inputs, userId, teamId, appId, null);
        
        // 将运行节点和边转换为调试响应格式
        List<String> finishedNodeIds = workflow.getNodes().stream()
            .map(Node::getNodeId)
            .collect(Collectors.toList());
        
        List<EdgeStatusDTO> edgeStatusList = workflow.getEdges().stream()
            .map(edge -> {
                EdgeStatusDTO.EdgeStatusEnum status;
                try {
                    status = EdgeStatusDTO.EdgeStatusEnum.valueOf(
                        (edge.getStatus() != null ? edge.getStatus() : "COMPLETED").toUpperCase()
                    );
                } catch (IllegalArgumentException e) {
                    status = EdgeStatusDTO.EdgeStatusEnum.ACTIVE;
                }
                return EdgeStatusDTO.builder()
                    .id(edge.getEdgeId())
                    .status(status)
                    .build();
            })
            .collect(Collectors.toList());
        
        // 创建调试响应
        WorkflowDebugResponse debugResponse = new WorkflowDebugResponse();
        debugResponse.setFinishedNodes(finishedNodeIds);
        debugResponse.setFinishedEdges(edgeStatusList);
        debugResponse.setNextStepRunNodes(Collections.emptyList());
        
        // 添加输出
        Map<String, Object> outMap = new HashMap<>(outputs);
        outMap.put("success", true);
        
        return debugResponse;
    }

    /**
     * 执行工作流
     */
    private List<String> executeWorkflow(
        List<Node> runtimeNodes, 
        List<Edge> runtimeEdges,
        Map<String, Object> variables,
        int remainingRuns,
        List<Map<String, Object>> flowResponses,
        List<Map<String, Object>> flowUsages,
        BiConsumer<String, Boolean> streamConsumer,
        Map<String, Object> finalOutputs
    ) {
        // 找到入口节点
        List<Node> entryNodes = runtimeNodes.stream()
            .filter(Node::isEntry)
            .collect(Collectors.toList());
        
        // 重置非交互式节点的入口状态
        runtimeNodes.forEach(node -> {
            if (!isInteractiveNode(node.getType().toString())) {
                node.setEntry(false);
            }
        });
        
        List<String> executedNodeIds = new ArrayList<>();
        Set<String> skippedNodeIds = new HashSet<>();
        
        // 执行所有入口节点
        for (Node node : entryNodes) {
            // 执行节点和下游节点
            executeNodeAndDownstream(
                node, 
                runtimeNodes, 
                runtimeEdges, 
                variables, 
                remainingRuns, 
                flowResponses, 
                flowUsages, 
                streamConsumer, 
                executedNodeIds, 
                skippedNodeIds
            );
            
            // 收集输出
            for (Node executedNode : runtimeNodes) {
                if (executedNodeIds.contains(executedNode.getNodeId())) {
                    for (NodeOutput output : executedNode.getOutputs()) {
                        if (output.getValue() != null) {
                            finalOutputs.put(output.getKey(), output.getValue());
                        }
                    }
                }
            }
        }
        
        return executedNodeIds;
    }
    
    /**
     * 执行节点和下游节点
     */
    private void executeNodeAndDownstream(
        Node node,
        List<Node> runtimeNodes,
        List<Edge> runtimeEdges,
        Map<String, Object> variables,
        int remainingRuns,
        List<Map<String, Object>> flowResponses,
        List<Map<String, Object>> flowUsages,
        BiConsumer<String, Boolean> streamConsumer,
        List<String> executedNodeIds,
        Set<String> skippedNodeIds
    ) {
        // 检查运行次数限制
        if (remainingRuns <= 0) {
            return;
        }
        
        // 检查节点是否已经执行过
        if (executedNodeIds.contains(node.getNodeId()) || skippedNodeIds.contains(node.getNodeId())) {
            return;
        }
        
        // 检查节点运行状态
        String status = checkNodeRunStatus(node, runtimeEdges);
        
        if ("run".equals(status)) {
            // 初始化边状态
            initNodeEdges(node, runtimeEdges);
            
            // 执行节点
            long nodeStartTime = System.currentTimeMillis();
            Map<String, Object> result = executeNode(node, runtimeNodes, variables);
            long nodeExecutionTime = System.currentTimeMillis() - nodeStartTime;
            
            // 跟踪节点执行
            trackNodeExecution(variables, node, nodeExecutionTime);
            
            // 记录已执行的节点
            executedNodeIds.add(node.getNodeId());
            
            // 处理结果
            if (result != null) {
                processNodeResult(
                    node, 
                    result, 
                    variables, 
                    flowResponses, 
                    flowUsages, 
                    streamConsumer
                );
            }
            
            // 获取并执行下一步节点
            Map<String, List<Node>> nextNodes = getNextNodes(
                node, 
                result, 
                runtimeNodes, 
                runtimeEdges
            );
            
            // 执行下一步激活节点
            for (Node nextNode : nextNodes.get("active")) {
                executeNodeAndDownstream(
                    nextNode, 
                    runtimeNodes, 
                    runtimeEdges, 
                    variables, 
                    remainingRuns - 1, 
                    flowResponses, 
                    flowUsages, 
                    streamConsumer, 
                    executedNodeIds, 
                    skippedNodeIds
                );
            }
            
            // 执行下一步跳过节点
            for (Node nextNode : nextNodes.get("skipped")) {
                executeNodeAndDownstream(
                    nextNode, 
                    runtimeNodes, 
                    runtimeEdges, 
                    variables, 
                    remainingRuns - 1, 
                    flowResponses, 
                    flowUsages, 
                    streamConsumer, 
                    executedNodeIds, 
                    skippedNodeIds
                );
            }
        } else if ("skip".equals(status)) {
            // 记录跳过的节点
            skippedNodeIds.add(node.getNodeId());
            
            // 标记所有出边为skip
            for (Edge edge : runtimeEdges) {
                if (edge.getSource().equals(node.getNodeId())) {
                    edge.setStatus("skipped");
                }
            }
            
            // 获取下一步节点
            List<Node> nextNodes = runtimeNodes.stream()
                .filter(n -> runtimeEdges.stream()
                    .anyMatch(e -> e.getSource().equals(node.getNodeId()) 
                        && e.getTarget().equals(n.getNodeId())))
                .collect(Collectors.toList());
            
            // 执行下一步节点
            for (Node nextNode : nextNodes) {
                if (!executedNodeIds.contains(nextNode.getNodeId()) 
                    && !skippedNodeIds.contains(nextNode.getNodeId())) {
                    executeNodeAndDownstream(
                        nextNode, 
                        runtimeNodes, 
                        runtimeEdges, 
                        variables, 
                        remainingRuns - 1, 
                        flowResponses, 
                        flowUsages, 
                        streamConsumer, 
                        executedNodeIds, 
                        skippedNodeIds
                    );
                }
            }
        }
    }
    
    /**
     * 执行单个节点
     */
    private Map<String, Object> executeNode(Node node, List<Node> runtimeNodes, Map<String, Object> variables) {
        log.debug("Executing node: {}", node.getName());
        
        try {
            // 获取节点参数
            Map<String, Object> params = getNodeParams(node, runtimeNodes, variables);
            
            // 获取节点调度器
            NodeDispatcher dispatcher = nodeDispatcherRegistry.getDispatcher(node.getType().toString());
            if (dispatcher == null) {
                throw new IllegalArgumentException("No dispatcher found for node type: " + node.getType());
            }
            
            // 执行节点
            NodeOutDTO outDTO = dispatcher.dispatch(node, params);
            
            // 转换结果
            Map<String, Object> result = new HashMap<>();
            if (outDTO != null) {
                // 添加输出
                if (outDTO.getOutput() != null) {
                    result.putAll(outDTO.getOutput());
                }
                
                // 添加响应数据
                if (outDTO.getResponseData() != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("nodeId", node.getNodeId());
                    responseData.put("moduleName", node.getName());
                    responseData.put("moduleType", node.getType().toString());
                    responseData.putAll(outDTO.getResponseData());
                    
                    result.put("responseData", responseData);
                }
                
                // 添加使用统计
                if (outDTO.getUsages() != null) {
                    result.put("nodeDispatchUsages", outDTO.getUsages());
                }
                
                // 添加新变量
                if (outDTO.getNewVariables() != null) {
                    result.put("newVariables", outDTO.getNewVariables());
                }
            }
            
            // 更新节点输出
            updateNodeOutputs(node, result);
            
            return result;
        } catch (Exception e) {
            log.error("Error executing node: {}", node.getName(), e);
            
            // 构建错误结果
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            
            return errorResult;
        }
    }
    
    /**
     * 处理节点结果
     */
    private void processNodeResult(
        Node node,
        Map<String, Object> result,
        Map<String, Object> variables,
        List<Map<String, Object>> flowResponses,
        List<Map<String, Object>> flowUsages,
        BiConsumer<String, Boolean> streamConsumer
    ) {
        // 保存响应数据
        if (result.get("responseData") != null) {
            flowResponses.add((Map<String, Object>) result.get("responseData"));
        }
        
        // 保存使用统计
        if (result.get("nodeDispatchUsages") != null) {
            flowUsages.add((Map<String, Object>) result.get("nodeDispatchUsages"));
        }
        
        // 更新变量
        if (result.get("newVariables") != null) {
            variables.putAll((Map<String, Object>) result.get("newVariables"));
        }
        
        // 处理流式输出
        if (streamConsumer != null && result.get("answerText") != null) {
            String text = (String) result.get("answerText");
            if (text != null && !text.isEmpty()) {
                streamConsumer.accept(text, false);
            }
        }
    }
    
    /**
     * 获取下一步节点
     */
    private Map<String, List<Node>> getNextNodes(
        Node node, 
        Map<String, Object> result, 
        List<Node> runtimeNodes, 
        List<Edge> runtimeEdges
    ) {
        // 获取跳过的源句柄
        List<String> skipHandleIds = result != null && result.containsKey("skipHandleId") 
            ? (List<String>) result.get("skipHandleId") 
            : Collections.emptyList();
        
        // 获取出边
        List<Edge> outEdges = runtimeEdges.stream()
            .filter(edge -> edge.getSource().equals(node.getNodeId()))
            .collect(Collectors.toList());
        
        // 更新边状态
        for (Edge edge : outEdges) {
            if (skipHandleIds.contains(edge.getSourceHandle())) {
                edge.setStatus("skipped");
            } else {
                edge.setStatus("active");
            }
        }
        
        // 获取下一步激活节点
        List<Node> activeNodes = runtimeNodes.stream()
            .filter(n -> outEdges.stream()
                .anyMatch(e -> e.getTarget().equals(n.getNodeId()) && "active".equals(e.getStatus())))
            .collect(Collectors.toList());
        
        // 获取下一步跳过节点
        List<Node> skippedNodes = runtimeNodes.stream()
            .filter(n -> outEdges.stream()
                .anyMatch(e -> e.getTarget().equals(n.getNodeId()) && "skipped".equals(e.getStatus())))
            .collect(Collectors.toList());
        
        Map<String, List<Node>> nextNodes = new HashMap<>();
        nextNodes.put("active", activeNodes);
        nextNodes.put("skipped", skippedNodes);
        
        return nextNodes;
    }
    
    /**
     * 获取节点参数
     */
    private Map<String, Object> getNodeParams(Node node, List<Node> runtimeNodes, Map<String, Object> variables) {
        Map<String, Object> params = new HashMap<>();
        
        // 查找动态输入
        NodeInput dynamicInput = node.getInputs().stream()
            .filter(input -> "addInputParam".equals(input.getRenderType()))
            .findFirst()
            .orElse(null);
        
        if (dynamicInput != null) {
            params.put(dynamicInput.getKey(), new HashMap<>());
        }
        
        // 处理所有输入
        for (NodeInput input : node.getInputs()) {
            // 跳过动态输入键
            if (dynamicInput != null && input.getKey().equals(dynamicInput.getKey())) {
                continue;
            }
            
            // 特殊键直接使用
            if (Arrays.asList("childrenNodeIdList", "httpJsonBody").contains(input.getKey())) {
                params.put(input.getKey(), input.getValue());
                continue;
            }
            
            // 替换变量
            Object value = variableManager.replaceVariables(input.getValue(), runtimeNodes, variables);
            
            // 动态输入处理
            if (input.isCanEdit() && dynamicInput != null && params.containsKey(dynamicInput.getKey())) {
                Map<String, Object> dynamicParams = (Map<String, Object>) params.get(dynamicInput.getKey());
                dynamicParams.put(input.getKey(), formatValue(value, input.getValueType()));
            }
            
            // 保存参数
            params.put(input.getKey(), formatValue(value, input.getValueType()));
        }
        
        return params;
    }
    
    /**
     * 格式化值
     */
    private Object formatValue(Object value, String valueType) {
        if (value == null) return null;
        
        switch (valueType) {
            case "boolean":
                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                } else if (value instanceof Boolean) {
                    return value;
                }
                return false;
                
            case "number":
                if (value instanceof String) {
                    try {
                        return Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0;
                
            case "json":
                if (value instanceof String) {
                    return value; // 简化处理
                } else if (value instanceof Map) {
                    return value;
                }
                return "{}";
                
            default:
                return String.valueOf(value);
        }
    }
    
    /**
     * 更新节点输出值
     */
    private void updateNodeOutputs(Node node, Map<String, Object> result) {
        if (result == null) return;
        
        for (NodeOutput output : node.getOutputs()) {
            if (result.containsKey(output.getKey())) {
                output.setValue(result.get(output.getKey()));
            }
        }
    }
    
    /**
     * 初始化节点边状态
     */
    private void initNodeEdges(Node node, List<Edge> runtimeEdges) {
        for (Edge edge : runtimeEdges) {
            if (edge.getTarget().equals(node.getNodeId())) {
                edge.setStatus("waiting");
            }
        }
    }
    
    /**
     * 检查节点运行状态
     */
    private String checkNodeRunStatus(Node node, List<Edge> runtimeEdges) {
        // 入口节点直接运行
        if (node.isEntry()) {
            return "run";
        }
        
        // 获取进入该节点的边
        List<Edge> inputEdges = runtimeEdges.stream()
            .filter(edge -> edge.getTarget().equals(node.getNodeId()))
            .collect(Collectors.toList());
        
        // 没有输入边则跳过
        if (inputEdges.isEmpty()) {
            return "skip";
        }
        
        // 检查是否有激活的边
        boolean hasActiveEdge = inputEdges.stream()
            .anyMatch(edge -> "active".equals(edge.getStatus()));
        
        return hasActiveEdge ? "run" : "skip";
    }
    
    /**
     * 是否交互式节点
     */
    private boolean isInteractiveNode(String nodeType) {
        return Arrays.asList("userSelect", "formInput", "tools").contains(nodeType);
    }
    
    /**
     * 获取系统变量
     */
    private Map<String, Object> getSystemVariables(String userId, String teamId, String appId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("teamId", teamId);
        variables.put("appId", appId);
        variables.put("cTime", new Date());
        return variables;
    }
    
    /**
     * 跟踪节点执行
     */
    private void trackNodeExecution(Map<String, Object> variables, Node node, long executionTimeMs) {
        try {
            String userId = (String) variables.get("userId");
            String teamId = (String) variables.get("teamId");
            String appId = (String) variables.get("appId");
            
            if (userId != null && appId != null) {
                usageTrackingService.trackNodeExecution(
                    appId, teamId, userId, node.getType().toString(), executionTimeMs
                );
            }
        } catch (Exception e) {
            log.error("Error tracking node execution", e);
        }
    }
} 