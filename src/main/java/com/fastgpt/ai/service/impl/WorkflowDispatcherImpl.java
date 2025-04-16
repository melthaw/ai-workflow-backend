package com.fastgpt.ai.service.impl;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 主要工作流调度器实现，类似于Next.js版本的dispatchWorkFlow函数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDispatcherImpl implements WorkflowDispatcher {
    private final NodeDispatcherRegistry nodeDispatcherRegistry;
    private final VariableManager variableManager;
    private final UsageTrackingService usageTrackingService;
    
    // 默认使用来源
    private static final String DEFAULT_SOURCE = "fastgpt";

    @Override
    public Map<String, Object> dispatchWorkflow(WorkflowDTO workflow, 
                                              Map<String, Object> inputs,
                                              String userId,
                                              String teamId,
                                              String appId,
                                              BiConsumer<String, Boolean> streamConsumer) {
        // 最大运行次数控制
        int maxRunTimes = 50;
        // 保存节点响应
        List<Map<String, Object>> flowResponses = new ArrayList<>();
        // 保存资源使用情况
        List<Map<String, Object>> flowUsages = new ArrayList<>();
        // 初始化变量(包括系统变量和输入变量)
        Map<String, Object> variables = new HashMap<>(inputs);
        variables.putAll(getSystemVariables(userId, teamId, appId));
        
        // 开始时间
        long startTime = System.currentTimeMillis();
        
        // 所有节点和边的运行时副本
        List<Node> runtimeNodes = new ArrayList<>(workflow.getNodes());
        List<Edge> runtimeEdges = new ArrayList<>(workflow.getEdges());
        
        // 总输出结果
        Map<String, Object> finalOutputs = new HashMap<>();
        
        try {
            // 找到入口节点
            List<Node> entryNodes = runtimeNodes.stream()
                .filter(Node::isEntry)
                .collect(Collectors.toList());
            
            // 重置入口状态（除了特殊节点）
            runtimeNodes.forEach(node -> {
                if (!isInteractiveNode(node.getType().toString())) {
                    node.setEntry(false);
                }
            });
            
            // 运行所有入口节点
            for (Node node : entryNodes) {
                Set<String> skippedNodeIdList = new HashSet<>();
                List<Node> executedNodes = runNode(node, runtimeNodes, runtimeEdges, variables, 
                    maxRunTimes, flowResponses, flowUsages, streamConsumer, skippedNodeIdList);
                
                // 更新最终输出
                for (Node executedNode : executedNodes) {
                    for (NodeOutput output : executedNode.getOutputs()) {
                        if (output.getValue() != null) {
                            finalOutputs.put(output.getKey(), output.getValue());
                        }
                    }
                }
            }
            
            // 跟踪工作流使用情况
            trackWorkflowUsage(workflow, userId, teamId, appId, flowUsages);
            
            return finalOutputs;
        } catch (Exception e) {
            log.error("Error dispatching workflow", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        } finally {
            // 记录工作流执行时间
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
        List<Node> finishedNodes = new ArrayList<>();
        List<Edge> finishedEdges = new ArrayList<>();
        List<Node> nextStepRunNodes = new ArrayList<>();
        Map<String, Object> variables = new HashMap<>(inputs);
        variables.putAll(getSystemVariables(userId, teamId, appId));
        
        // 创建调试响应
        WorkflowDebugResponse debugResponse = new WorkflowDebugResponse();
        debugResponse.setFinishedNodes(finishedNodes);
        debugResponse.setFinishedEdges(finishedEdges);
        debugResponse.setNextStepRunNodes(nextStepRunNodes);
        
        // 运行工作流
        Map<String, Object> outputs = dispatchWorkflow(workflow, inputs, userId, teamId, appId, null);
        
        // 设置调试信息
        debugResponse.setOutputs(outputs);
        return debugResponse;
    }

    /**
     * 运行节点并获取下一步节点
     */
    private List<Node> runNode(Node node, 
                              List<Node> runtimeNodes, 
                              List<Edge> runtimeEdges,
                              Map<String, Object> variables,
                              int maxRunTimes,
                              List<Map<String, Object>> flowResponses,
                              List<Map<String, Object>> flowUsages,
                              BiConsumer<String, Boolean> streamConsumer,
                              Set<String> skippedNodeIdList) {
        // 检查节点运行状态
        String status = checkNodeRunStatus(node, runtimeEdges);
        
        if (maxRunTimes <= 0) {
            return Collections.emptyList();
        }
        
        List<Node> executedNodes = new ArrayList<>();
        
        // 根据状态执行节点
        if ("run".equals(status)) {
            initNodeEdges(node, runtimeEdges);
            log.debug("Running node: {}", node.getName());
            
            long nodeStartTime = System.currentTimeMillis();
            NodeResult result = runNodeWithActive(node, runtimeNodes, runtimeEdges, variables);
            long nodeExecutionTime = System.currentTimeMillis() - nodeStartTime;
            
            // 跟踪节点执行
            trackNodeExecution(variables, node, nodeExecutionTime);
            
            executedNodes.add(node);
            
            // 保存结果
            if (result.getResult() != null) {
                if (result.getResult().get("responseData") != null) {
                    flowResponses.add((Map<String, Object>) result.getResult().get("responseData"));
                }
                if (result.getResult().get("nodeDispatchUsages") != null) {
                    flowUsages.add((Map<String, Object>) result.getResult().get("nodeDispatchUsages"));
                }
                
                // 更新变量
                if (result.getResult().get("newVariables") != null) {
                    variables.putAll((Map<String, Object>) result.getResult().get("newVariables"));
                }
                
                // 处理流式输出
                if (streamConsumer != null && result.getResult().get("answerText") != null) {
                    String text = (String) result.getResult().get("answerText");
                    if (text != null && !text.isEmpty()) {
                        streamConsumer.accept(text, false);
                    }
                }
            }
            
            // 更新节点输出
            updateNodeOutputs(node, result.getResult());
            
            // 获取下一步节点
            NodeNextStep nextStep = getNextStepNodes(node, result.getResult(), runtimeNodes, runtimeEdges);
            
            // 执行下一步活动节点
            for (Node nextNode : nextStep.getNextStepActiveNodes()) {
                List<Node> nextExecutedNodes = runNode(nextNode, runtimeNodes, runtimeEdges,
                    variables, maxRunTimes - 1, flowResponses, flowUsages, streamConsumer, skippedNodeIdList);
                executedNodes.addAll(nextExecutedNodes);
            }
            
            // 执行下一步跳过节点
            for (Node nextNode : nextStep.getNextStepSkipNodes()) {
                if (!executedNodes.contains(nextNode) && !skippedNodeIdList.contains(nextNode.getNodeId())) {
                    List<Node> nextExecutedNodes = runNode(nextNode, runtimeNodes, runtimeEdges,
                        variables, maxRunTimes - 1, flowResponses, flowUsages, streamConsumer, skippedNodeIdList);
                    executedNodes.addAll(nextExecutedNodes);
                }
            }
        } else if ("skip".equals(status) && !skippedNodeIdList.contains(node.getNodeId())) {
            initNodeEdges(node, runtimeEdges);
            log.debug("Skipping node: {}", node.getName());
            skippedNodeIdList.add(node.getNodeId());
            
            // 标记所有出边为skip
            NodeResult result = runNodeWithSkip(node);
            executedNodes.add(node);
            
            // 获取下一步节点
            NodeNextStep nextStep = getNextStepNodes(node, result.getResult(), runtimeNodes, runtimeEdges);
            
            // 执行下一步节点（全部以skip方式）
            for (Node nextNode : nextStep.getNextStepActiveNodes()) {
                if (!executedNodes.contains(nextNode) && !skippedNodeIdList.contains(nextNode.getNodeId())) {
                    List<Node> nextExecutedNodes = runNode(nextNode, runtimeNodes, runtimeEdges,
                        variables, maxRunTimes - 1, flowResponses, flowUsages, streamConsumer, skippedNodeIdList);
                    executedNodes.addAll(nextExecutedNodes);
                }
            }
            
            for (Node nextNode : nextStep.getNextStepSkipNodes()) {
                if (!executedNodes.contains(nextNode) && !skippedNodeIdList.contains(nextNode.getNodeId())) {
                    List<Node> nextExecutedNodes = runNode(nextNode, runtimeNodes, runtimeEdges,
                        variables, maxRunTimes - 1, flowResponses, flowUsages, streamConsumer, skippedNodeIdList);
                    executedNodes.addAll(nextExecutedNodes);
                }
            }
        }
        
        return executedNodes;
    }
    
    /**
     * 主动运行节点
     */
    private NodeResult runNodeWithActive(Node node, List<Node> runtimeNodes, List<Edge> runtimeEdges, Map<String, Object> variables) {
        // 获取节点运行参数
        Map<String, Object> params = getNodeRunParams(node, runtimeNodes, variables);
        
        try {
            // 获取节点调度器
            NodeDispatcher dispatcher = nodeDispatcherRegistry.getDispatcher(node.getType().toString());
            if (dispatcher == null) {
                throw new IllegalArgumentException("No dispatcher found for node type: " + node.getType());
            }
            
            // 调度节点执行
            NodeOutDTO outDTO = dispatcher.dispatch(node, params);
            
            // 转换结果格式
            Map<String, Object> result = new HashMap<>();
            if (outDTO != null) {
                // 添加基本输出
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
                
                // 添加资源使用情况
                if (outDTO.getUsages() != null) {
                    result.put("nodeDispatchUsages", outDTO.getUsages());
                }
                
                // 添加新变量
                if (outDTO.getNewVariables() != null) {
                    result.put("newVariables", outDTO.getNewVariables());
                }
            }
            
            return new NodeResult(node, "run", result);
        } catch (Exception e) {
            log.error("Error running node: {}", node.getName(), e);
            
            // 获取所有出边的源句柄
            List<String> skipHandleIds = runtimeEdges.stream()
                .filter(edge -> edge.getSourceNodeId().equals(node.getNodeId()))
                .map(Edge::getSourceHandle)
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("error", e.getMessage());
            result.put("skipHandleId", skipHandleIds);
            
            return new NodeResult(node, "error", result);
        }
    }
    
    /**
     * 以跳过方式处理节点
     */
    private NodeResult runNodeWithSkip(Node node) {
        // 获取所有出边的源句柄
        List<String> skipHandleIds = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("skipHandleId", skipHandleIds);
        
        return new NodeResult(node, "skip", result);
    }
    
    /**
     * 获取节点运行参数，处理变量替换
     */
    private Map<String, Object> getNodeRunParams(Node node, List<Node> runtimeNodes, Map<String, Object> variables) {
        Map<String, Object> params = new HashMap<>();
        
        // 处理动态输入
        NodeInput dynamicInput = node.getInputs().stream()
            .filter(input -> "addInputParam".equals(input.getRenderType()))
            .findFirst()
            .orElse(null);
        
        if (dynamicInput != null) {
            params.put(dynamicInput.getKey(), new HashMap<String, Object>());
        }
        
        // 处理所有输入
        for (NodeInput input : node.getInputs()) {
            // 特殊输入处理
            if (dynamicInput != null && input.getKey().equals(dynamicInput.getKey())) {
                continue;
            }
            
            // 特殊键不需要处理
            if (Arrays.asList("childrenNodeIdList", "httpJsonBody").contains(input.getKey())) {
                params.put(input.getKey(), input.getValue());
                continue;
            }
            
            // 替换变量
            Object value = variableManager.replaceVariables(input.getValue(), runtimeNodes, variables);
            
            // 处理动态输入
            if (input.isCanEdit() && dynamicInput != null && params.containsKey(dynamicInput.getKey())) {
                Map<String, Object> dynamicParams = (Map<String, Object>) params.get(dynamicInput.getKey());
                dynamicParams.put(input.getKey(), formatValue(value, input.getValueType()));
            }
            
            params.put(input.getKey(), formatValue(value, input.getValueType()));
        }
        
        return params;
    }
    
    /**
     * 根据值类型格式化值
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
                    try {
                        // 简化处理，实际需要使用JSON库
                        return value;
                    } catch (Exception e) {
                        return "{}";
                    }
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
        // 如果是入口节点，直接运行
        if (node.isEntry()) {
            return "run";
        }
        
        // 获取进入该节点的所有边
        List<Edge> inputEdges = runtimeEdges.stream()
            .filter(edge -> edge.getTarget().equals(node.getNodeId()))
            .collect(Collectors.toList());
        
        // 如果没有输入边，跳过节点
        if (inputEdges.isEmpty()) {
            return "skip";
        }
        
        // 检查是否有激活的边
        boolean hasActiveEdge = inputEdges.stream()
            .anyMatch(edge -> "active".equals(edge.getStatus()));
        
        return hasActiveEdge ? "run" : "skip";
    }
    
    /**
     * 获取下一步节点
     */
    private NodeNextStep getNextStepNodes(Node node, Map<String, Object> result, List<Node> runtimeNodes, List<Edge> runtimeEdges) {
        List<String> skipHandleId = result != null && result.containsKey("skipHandleId") ? 
            (List<String>) result.get("skipHandleId") : Collections.emptyList();
        
        // 获取从该节点出发的所有边
        List<Edge> targetEdges = runtimeEdges.stream()
            .filter(edge -> edge.getSource().equals(node.getNodeId()))
            .collect(Collectors.toList());
        
        // 更新边状态
        for (Edge edge : targetEdges) {
            if (skipHandleId.contains(edge.getSourceHandle())) {
                edge.setStatus("skipped");
            } else {
                edge.setStatus("active");
            }
        }
        
        // 获取下一步激活节点
        List<Node> nextStepActiveNodes = runtimeNodes.stream()
            .filter(n -> targetEdges.stream()
                .anyMatch(e -> e.getTarget().equals(n.getNodeId()) && "active".equals(e.getStatus())))
            .collect(Collectors.toList());
        
        // 获取下一步跳过节点
        List<Node> nextStepSkipNodes = runtimeNodes.stream()
            .filter(n -> targetEdges.stream()
                .anyMatch(e -> e.getTarget().equals(n.getNodeId()) && "skipped".equals(e.getStatus())))
            .collect(Collectors.toList());
        
        return new NodeNextStep(nextStepActiveNodes, nextStepSkipNodes);
    }
    
    /**
     * 判断是否为交互式节点
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
     * 跟踪工作流使用情况
     */
    private void trackWorkflowUsage(WorkflowDTO workflow, String userId, String teamId, String appId, List<Map<String, Object>> flowUsages) {
        if (flowUsages == null || flowUsages.isEmpty()) {
            return;
        }
        
        try {
            // 使用UsageTrackingService跟踪资源使用
            usageTrackingService.trackWorkflowUsage(
                workflow.getName() != null ? workflow.getName() : "未命名工作流",
                appId,
                teamId,
                userId,
                DEFAULT_SOURCE,
                flowUsages
            );
        } catch (Exception e) {
            log.error("Error tracking workflow usage", e);
        }
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
                    appId,
                    teamId,
                    userId,
                    node.getType().toString(),
                    executionTimeMs
                );
            }
        } catch (Exception e) {
            log.error("Error tracking node execution", e);
        }
    }
    
    /**
     * 节点结果包装类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class NodeResult {
        private Node node;
        private String runStatus;
        private Map<String, Object> result;
    }
    
    /**
     * 下一步节点包装类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class NodeNextStep {
        private List<Node> nextStepActiveNodes;
        private List<Node> nextStepSkipNodes;
    }
} 