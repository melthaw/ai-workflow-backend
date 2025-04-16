package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.constant.FlowNodeTypeEnum;
import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.ConnectionDTO;
import com.fastgpt.ai.dto.request.WorkflowCreateRequest;
import com.fastgpt.ai.dto.request.WorkflowUpdateRequest;
import com.fastgpt.ai.entity.Workflow;
import com.fastgpt.ai.entity.workflow.Edge;
import com.fastgpt.ai.entity.workflow.Node;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.mapper.WorkflowMapper;
import com.fastgpt.ai.repository.WorkflowRepository;
import com.fastgpt.ai.service.AiService;
import com.fastgpt.ai.service.KnowledgeBaseService;
import com.fastgpt.ai.service.NodeDispatcherRegistry;
import com.fastgpt.ai.service.WorkflowMonitorService;
import com.fastgpt.ai.service.WorkflowService;
import com.fastgpt.ai.service.VectorService;
import com.fastgpt.ai.service.WorkflowInteractionService;
import com.fastgpt.ai.service.impl.workflow.IfElseDispatcher;
import com.fastgpt.ai.service.impl.workflow.LoopDispatcher;
import com.fastgpt.ai.service.impl.workflow.InteractiveNodeDispatcher;
import com.fastgpt.ai.service.impl.workflow.HttpRequestDispatcher;
import com.fastgpt.ai.service.impl.workflow.CodeExecutionDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of workflow execution service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowMapper workflowMapper;
    private final WorkflowMonitorService monitorService;
    private final WorkflowInteractionService interactionService;
    private final AiService aiService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorService vectorService;
    private final LoopDispatcher loopDispatcher;
    private final IfElseDispatcher ifElseDispatcher;
    private final InteractiveNodeDispatcher interactiveNodeDispatcher;
    private final HttpRequestDispatcher httpRequestDispatcher;
    private final CodeExecutionDispatcher codeExecutionDispatcher;
    private final NodeDispatcherRegistry nodeDispatcherRegistry;
    
    // Map of node type to node handler functions (enhanced with new node types)
    private final Map<String, Function<NodeExecutionContext, NodeOutDTO>> nodeHandlers = initNodeHandlers();

    // Cache for active workflow streams
    private final Map<String, StreamContext> activeStreams = new ConcurrentHashMap<>();

    private static final int MAX_HEARTBEAT_COUNT = 300; // 5分钟的最大心跳次数
    private static final long HEARTBEAT_INTERVAL_MS = 10000; // 10秒心跳间隔

    /**
     * Constructor that initializes the node handlers map
     */
    @Autowired
    public WorkflowServiceImpl(
            WorkflowRepository workflowRepository,
            WorkflowMapper workflowMapper,
            WorkflowMonitorService monitorService,
            WorkflowInteractionService interactionService,
            AiService aiService) {
        this.workflowRepository = workflowRepository;
        this.workflowMapper = workflowMapper;
        this.monitorService = monitorService;
        this.interactionService = interactionService;
        this.aiService = aiService;
        this.nodeHandlers = initializeNodeHandlers();
    }

    /**
     * Initialize node handlers map with support for new node types
     */
    private Map<String, Function<NodeExecutionContext, NodeOutDTO>> initNodeHandlers() {
        Map<String, Function<NodeExecutionContext, NodeOutDTO>> handlers = new HashMap<>();
        
        // Base node types
        handlers.put("workflowStart", this::handleStartNode);
        handlers.put("workflowEnd", this::handleEndNode);
        
        // AI nodes
        handlers.put("chatNode", this::handleChatNode);
        handlers.put("textGenerationNode", this::handleTextGenerationNode);
        
        // Data nodes
        handlers.put("datasetSearchNode", this::handleDatasetSearchNode);
        handlers.put("vectorSearchNode", this::handleVectorSearchNode);
        handlers.put("dataTransformNode", this::handleDataTransformNode);
        
        // Logic nodes
        handlers.put("ifElseNode", this::handleIfElseNode);
        handlers.put("switchNode", this::handleSwitchNode);
        handlers.put("loopNode", this::handleLoopNode);
        
        // Function nodes
        handlers.put("functionNode", this::handleFunctionNode);
        handlers.put("codeNode", this::handleCodeNode);
        
        // Text editor node
        handlers.put("textEditor", this::handleTextEditorNode);
        
        // Custom feedback node
        handlers.put("customFeedback", this::handleCustomFeedbackNode);
        
        return handlers;
    }

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
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "workflowId", workflowId));
        
        return convertToDTO(workflow);
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
        String executionId = monitorService.startExecution(workflowId, inputs);
        
        try {
            Map<String, Object> result = dispatchWorkflow(workflow, inputs, null);
            monitorService.completeExecution(executionId, result);
            return result;
        } catch (Exception e) {
            log.error("Error executing workflow: {}", workflowId, e);
            monitorService.failExecution(executionId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Async
    public CompletableFuture<Map<String, Object>> executeWorkflowAsync(String workflowId, Map<String, Object> inputs) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        
        try {
            Map<String, Object> result = executeWorkflow(workflowId, inputs);
            future.complete(result);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public Map<String, Object> dispatchWorkflow(WorkflowDTO workflow, Map<String, Object> inputs, String startNodeId) {
        log.info("Dispatching workflow: {}", workflow.getWorkflowId());
        
        // Initialize context with inputs
        Map<String, Object> context = new HashMap<>(inputs);
        
        // Set default start nodes (entry points)
        List<Node> nodesToProcess = new ArrayList<>();
        
        if (startNodeId != null) {
            // Start from specific node
            workflow.getNodes().stream()
                    .filter(node -> node.getId().equals(startNodeId))
                    .findFirst()
                    .ifPresent(nodesToProcess::add);
            
            if (nodesToProcess.isEmpty()) {
                throw new IllegalArgumentException("Start node not found: " + startNodeId);
            }
        } else {
            // Start from entry nodes (nodes with no incoming edges)
            Set<String> targetNodeIds = workflow.getEdges().stream()
                    .map(Edge::getTarget)
                    .collect(java.util.stream.Collectors.toSet());
            
            workflow.getNodes().stream()
                    .filter(node -> !targetNodeIds.contains(node.getId()))
                    .forEach(nodesToProcess::add);
        }
        
        // Process nodes in sequence
        Set<String> processedNodes = new HashSet<>();
        
        while (!nodesToProcess.isEmpty()) {
            Node node = nodesToProcess.remove(0);
            
            if (processedNodes.contains(node.getId())) {
                continue; // Skip already processed nodes
            }
            
            // Check if all dependencies are processed
            boolean dependenciesMet = workflow.getEdges().stream()
                    .filter(edge -> edge.getTarget().equals(node.getId()))
                    .allMatch(edge -> processedNodes.contains(edge.getSource()));
            
            if (!dependenciesMet && startNodeId == null) {
                // Put back in queue to process later
                nodesToProcess.add(node);
                continue;
            }
            
            // Execute node
            log.info("Executing node: {}", node.getId());
            NodeOutDTO nodeResult = executeNode(workflow, node.getId(), context);
            
            if (nodeResult.isSuccess()) {
                // Update context with node outputs
                if (nodeResult.getOutputs() != null) {
                    context.putAll(nodeResult.getOutputs());
                }
                
                // Mark as processed
                processedNodes.add(node.getId());
                
                // Add child nodes to process queue
                workflow.getEdges().stream()
                        .filter(edge -> edge.getSource().equals(node.getId()))
                        .map(Edge::getTarget)
                        .distinct()
                        .flatMap(targetId -> workflow.getNodes().stream()
                                .filter(n -> n.getId().equals(targetId)))
                        .forEach(nodesToProcess::add);
            } else {
                throw new RuntimeException("Node execution failed: " + nodeResult.getError());
            }
        }
        
        return context;
    }

    @Override
    public NodeOutDTO executeNode(WorkflowDTO workflow, String nodeId, Map<String, Object> inputs) {
        Node node = workflow.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
        
        // Find node dispatcher for the node type
        return nodeDispatcherRegistry.dispatchNode(node, inputs);
    }

    @Override
    public List<WorkflowDTO> getWorkflowsByUserIdOrTeamId(String userId, String teamId) {
        return workflowMapper.toDTOList(workflowRepository.findByUserIdOrTeamId(userId, teamId));
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
        log.info("Executing function node: {}", context.getNodeId());
        
        try {
            // Get function information from node properties
            Map<String, Object> properties = context.getNodeProperties();
            String functionId = getRequiredProperty(properties, "functionId", String.class);
            String functionName = getRequiredProperty(properties, "functionName", String.class);
            
            // Get input arguments
            Map<String, Object> args = new HashMap<>();
            Map<String, Object> inputs = context.getInputs();
            
            // Process mapped arguments
            @SuppressWarnings("unchecked")
            Map<String, String> argumentMappings = (Map<String, String>) properties.getOrDefault("argumentMappings", Collections.emptyMap());
            
            // Map inputs to function arguments based on mapping configuration
            for (Map.Entry<String, String> mapping : argumentMappings.entrySet()) {
                String argName = mapping.getKey();
                String inputKey = mapping.getValue();
                
                if (inputs.containsKey(inputKey)) {
                    args.put(argName, inputs.get(inputKey));
                }
            }
            
            // Add any direct arguments
            @SuppressWarnings("unchecked")
            Map<String, Object> directArgs = (Map<String, Object>) properties.getOrDefault("arguments", Collections.emptyMap());
            args.putAll(directArgs);
            
            // Log the function call with args
            log.info("Calling function: {} (ID: {}) with arguments: {}", functionName, functionId, args);
            
            // Call the function
            FunctionCallResult result = functionCallService.callFunction(functionId, args);
            
            if (!result.isSuccess()) {
                String errorMessage = "Function call failed: " + result.getError();
                log.error(errorMessage);
                return createErrorResult(context, errorMessage);
            }
            
            // Map outputs
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", result.getResult());
            outputs.put("success", result.isSuccess());
            outputs.put("executionTimeMs", result.getExecutionTimeMs());
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("functionId", functionId);
            metadata.put("functionName", functionName);
            metadata.put("arguments", args);
            if (result.getMetadata() != null) {
                metadata.putAll(result.getMetadata());
            }
            
            return createSuccessResult(context, outputs, metadata);
        } catch (Exception e) {
            log.error("Error in function node {}: {}", context.getNodeId(), e.getMessage(), e);
            return createErrorResult(context, "Function node error: " + e.getMessage());
        }
    }
    
    /**
     * Get a required property from node properties
     */
    private <T> T getRequiredProperty(Map<String, Object> properties, String key, Class<T> type) {
        Object value = properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required property '" + key + "' is missing");
        }
        return convertValue(value, type);
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

    /**
     * Handler for if-else conditional node
     */
    private NodeOutDTO handleIfElseNode(NodeExecutionContext context) {
        log.info("Handling if-else node: {}", context.getNode().getId());
        
        // Extract data from NodeDefDTO to pass to IfElseDispatcher
        Map<String, Object> nodeData = context.getNode().getData() != null 
            ? context.getNode().getData() : new HashMap<>();
            
        // Create a temporary Node object to use with the dispatcher
        com.fastgpt.ai.entity.workflow.Node tempNode = new com.fastgpt.ai.entity.workflow.Node();
        tempNode.setNodeId(context.getNode().getId());
        tempNode.setData(nodeData);
        
        return ifElseDispatcher.dispatchIfElse(tempNode, context.getInputs());
    }
    
    /**
     * Handler for loop node
     */
    private NodeOutDTO handleLoopNode(NodeExecutionContext context) {
        log.info("Handling loop node: {}", context.getNode().getId());
        
        // Extract data from NodeDefDTO to pass to LoopDispatcher
        Map<String, Object> nodeData = context.getNode().getData() != null 
            ? context.getNode().getData() : new HashMap<>();
            
        // Create a temporary Node object to use with the dispatcher
        com.fastgpt.ai.entity.workflow.Node tempNode = new com.fastgpt.ai.entity.workflow.Node();
        tempNode.setNodeId(context.getNode().getId());
        tempNode.setData(nodeData);
        
        return loopDispatcher.dispatchLoop(tempNode, context.getInputs());
    }
    
    /**
     * Handler for loop start node
     */
    private NodeOutDTO handleLoopStartNode(NodeExecutionContext context) {
        log.info("Handling loop start node: {}", context.getNode().getId());
        
        // Extract data from NodeDefDTO to pass to LoopDispatcher
        Map<String, Object> nodeData = context.getNode().getData() != null 
            ? context.getNode().getData() : new HashMap<>();
            
        // Create a temporary Node object to use with the dispatcher
        com.fastgpt.ai.entity.workflow.Node tempNode = new com.fastgpt.ai.entity.workflow.Node();
        tempNode.setNodeId(context.getNode().getId());
        tempNode.setData(nodeData);
        
        return loopDispatcher.dispatchLoopStart(tempNode, context.getInputs());
    }
    
    /**
     * Handler for loop end node
     */
    private NodeOutDTO handleLoopEndNode(NodeExecutionContext context) {
        log.info("Handling loop end node: {}", context.getNode().getId());
        
        // Extract data from NodeDefDTO to pass to LoopDispatcher
        Map<String, Object> nodeData = context.getNode().getData() != null 
            ? context.getNode().getData() : new HashMap<>();
            
        // Create a temporary Node object to use with the dispatcher
        com.fastgpt.ai.entity.workflow.Node tempNode = new com.fastgpt.ai.entity.workflow.Node();
        tempNode.setNodeId(context.getNode().getId());
        tempNode.setData(nodeData);
        
        return loopDispatcher.dispatchLoopEnd(tempNode, context.getInputs());
    }
    
    // Placeholder handlers for other new node types
    
    private NodeOutDTO handleWorkflowStartNode(NodeExecutionContext context) {
        // Simply pass through the inputs
        return NodeOutDTO.success(context.getInputs());
    }
    
    private NodeOutDTO handleAnswerNode(NodeExecutionContext context) {
        Map<String, Object> inputs = context.getInputs();
        Map<String, Object> outputs = new HashMap<>();
        
        // Extract answer from inputs
        if (inputs.containsKey("answer")) {
            outputs.put("answer", inputs.get("answer"));
        } else if (inputs.containsKey("text")) {
            outputs.put("answer", inputs.get("text"));
        } else {
            outputs.put("answer", "No answer available");
        }
        
        return NodeOutDTO.success(outputs);
    }
    
    private NodeOutDTO handleChatNode(NodeExecutionContext context) {
        // For now, delegate to the existing AI node handler
        return handleAiNode(context);
    }
    
    private NodeOutDTO handleDatasetSearchNode(NodeExecutionContext context) {
        // For now, delegate to the existing KB node handler
        return handleKbNode(context);
    }
    
    private NodeOutDTO handleDatasetConcatNode(NodeExecutionContext context) {
        // Placeholder implementation
        log.info("Dataset concat node not fully implemented yet");
        return NodeOutDTO.success(new HashMap<>());
    }

    /**
     * Handle userSelect node type
     * Delegates to interactive node dispatcher
     */
    private NodeOutDTO handleUserSelectNode(NodeExecutionContext context) {
        log.info("Processing user select node: {}", context.getNode().getId());
        
        // Extract data from NodeDefDTO to pass to InteractiveNodeDispatcher
        Map<String, Object> nodeData = context.getNode().getData() != null 
            ? context.getNode().getData() : new HashMap<>();
            
        // Create a temporary Node object to use with the dispatcher
        com.fastgpt.ai.entity.workflow.Node tempNode = new com.fastgpt.ai.entity.workflow.Node();
        tempNode.setNodeId(context.getNode().getId());
        tempNode.setData(nodeData);
        
        return interactiveNodeDispatcher.dispatchUserSelect(tempNode, context.getInputs());
    }

    /**
     * Handle formInput node type
     * Delegates to interactive node dispatcher
     */
    private NodeOutDTO handleFormInputNode(NodeExecutionContext context) {
        log.info("Processing form input node: {}", context.getNode().getId());
        
        // Extract data from NodeDefDTO to pass to InteractiveNodeDispatcher
        Map<String, Object> nodeData = context.getNode().getData() != null 
            ? context.getNode().getData() : new HashMap<>();
            
        // Create a temporary Node object to use with the dispatcher
        com.fastgpt.ai.entity.workflow.Node tempNode = new com.fastgpt.ai.entity.workflow.Node();
        tempNode.setNodeId(context.getNode().getId());
        tempNode.setData(nodeData);
        
        return interactiveNodeDispatcher.dispatchFormInput(tempNode, context.getInputs());
    }

    /**
     * Handle httpRequest node type
     * Delegates to HTTP request dispatcher
     */
    private NodeOutDTO handleHttpRequestNode(NodeExecutionContext context) {
        log.info("Processing HTTP request node: {}", context.getNode().getId());
        
        // Extract data from NodeDefDTO to pass to HttpRequestDispatcher
        Map<String, Object> nodeData = context.getNode().getData() != null 
            ? context.getNode().getData() : new HashMap<>();
            
        // Create a temporary Node object to use with the dispatcher
        com.fastgpt.ai.entity.workflow.Node tempNode = new com.fastgpt.ai.entity.workflow.Node();
        tempNode.setNodeId(context.getNode().getId());
        tempNode.setData(nodeData);
        
        return httpRequestDispatcher.dispatchHttpRequest(tempNode, context.getInputs());
    }

    /**
     * Handle code execution node type
     * Delegates to code execution dispatcher
     */
    private NodeOutDTO handleCodeNode(NodeExecutionContext context) {
        log.info("Processing code execution node: {}", context.getNode().getId());
        
        // Extract data from NodeDefDTO to pass to CodeExecutionDispatcher
        Map<String, Object> nodeData = context.getNode().getData() != null 
            ? context.getNode().getData() : new HashMap<>();
            
        // Create a temporary Node object to use with the dispatcher
        com.fastgpt.ai.entity.workflow.Node tempNode = new com.fastgpt.ai.entity.workflow.Node();
        tempNode.setNodeId(context.getNode().getId());
        tempNode.setData(nodeData);
        
        return codeExecutionDispatcher.dispatchCodeExecution(tempNode, context.getInputs());
    }

    @Override
    public Map<String, Object> getExecutionMetadata(String workflowId) {
        log.info("Getting execution metadata for workflow: {}", workflowId);
        
        // Create basic metadata map with available information
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("workflowId", workflowId);
        
        try {
            // Get workflow definition to include some basic details
            WorkflowDTO workflow = getWorkflowById(workflowId);
            if (workflow != null) {
                metadata.put("name", workflow.getName());
                metadata.put("nodeCount", workflow.getNodes() != null ? workflow.getNodes().size() : 0);
                metadata.put("edgeCount", workflow.getEdges() != null ? workflow.getEdges().size() : 0);
            }
            
            // Add execution statistics if available from monitoring service
            // Note: This assumes the monitoring service has methods to retrieve stats about workflow executions
            // In a real implementation, you would need to modify the monitoring service to provide this data
            metadata.put("lastExecutionTime", System.currentTimeMillis());
            metadata.put("status", "Available");
            
            return metadata;
        } catch (Exception e) {
            log.error("Error retrieving workflow execution metadata: {}", e.getMessage(), e);
            Map<String, Object> errorMetadata = new HashMap<>();
            errorMetadata.put("workflowId", workflowId);
            errorMetadata.put("error", e.getMessage());
            errorMetadata.put("status", "Error");
            return errorMetadata;
        }
    }
    
    @Override
    public void streamWorkflow(String workflowId, Map<String, Object> inputs, BiConsumer<String, Boolean> chunkConsumer) {
        log.info("Starting streaming workflow execution: {}", workflowId);
        
        // 获取工作流定义
        WorkflowDTO workflow = getWorkflowById(workflowId);
        
        // 初始化执行
        long startTime = System.currentTimeMillis();
        
        // 记录工作流开始
        String executionId = monitorService.recordWorkflowStart(workflowId, inputs);
        
        // 初始化工作流执行上下文
        Map<String, Object> executionContext = new HashMap<>();
        executionContext.put("__execution_id", executionId);
        executionContext.put("__workflow_id", workflowId);
        executionContext.put("__start_time", startTime);
        executionContext.put("__is_streaming", true);
        
        // 添加输入参数到上下文
        if (inputs != null) {
            executionContext.putAll(inputs);
        }
        
        // 添加默认输入值
        if (workflow.getDefaultInputs() != null) {
            for (Map.Entry<String, Object> entry : workflow.getDefaultInputs().entrySet()) {
                if (entry.getKey() != null && !executionContext.containsKey(entry.getKey())) {
                    executionContext.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        try {
            // 执行工作流并处理流式响应
            StreamingWorkflowExecutor executor = new StreamingWorkflowExecutor(
                    workflow, 
                    executionContext, 
                    chunkConsumer, 
                    monitorService,
                    executionId);
            
            executor.execute();
            
            // 记录工作流完成
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;
            monitorService.recordWorkflowComplete(executionId, workflowId, executor.getOutputs(), durationMs, executor.getVisitedNodes().size());
            
            log.info("Streaming workflow execution completed: {} (ID: {}) in {}ms, processed {} nodes", 
                    workflowId, executionId, durationMs, executor.getVisitedNodes().size());
            
        } catch (Exception e) {
            log.error("Error in streaming workflow execution", e);
            monitorService.recordWorkflowError(executionId, workflowId, e.getMessage(), 
                    System.currentTimeMillis() - startTime, 0);
            
            // 发送错误信息给客户端
            chunkConsumer.accept("Error: " + e.getMessage(), true);
        }
    }
    
    @Override
    public WorkflowDebugResponse getDebugInfo(String workflowId, String executionId) {
        log.info("Getting debug info for workflow execution: {} (ID: {})", workflowId, executionId);
        
        // 从监控服务获取工作流执行信息
        Map<String, Object> executionData = monitorService.getExecutionData(executionId);
        
        if (executionData == null || executionData.isEmpty()) {
            throw new ResourceNotFoundException("Workflow execution", "executionId", executionId);
        }
        
        // 构建调试响应
        WorkflowDebugResponse response = WorkflowDebugResponse.builder()
                .finishedNodes(getListFromMap(executionData, "finishedNodes", String.class))
                .nextStepRunNodes(getListFromMap(executionData, "nextNodes", String.class))
                .totalExecutionTimeMs(getLongFromMap(executionData, "durationMs", 0L))
                .processedNodesCount(getIntFromMap(executionData, "processedNodesCount", 0))
                .contextSnapshot(getMapFromMap(executionData, "contextSnapshot"))
                .errors(getListFromMap(executionData, "errors", String.class))
                .build();
        
        // 获取边缘状态信息
        List<Map<String, Object>> edgesData = getListFromMap(executionData, "edgeStatuses", Map.class);
        if (edgesData != null) {
            List<EdgeStatusDTO> edgeStatuses = edgesData.stream()
                    .map(this::mapToEdgeStatus)
                    .collect(Collectors.toList());
            response.setFinishedEdges(edgeStatuses);
        }
        
        // 获取节点执行指标
        Map<String, Object> nodeMetricsMap = getMapFromMap(executionData, "nodeMetrics");
        if (nodeMetricsMap != null) {
            Map<String, NodeExecutionMetrics> metrics = new HashMap<>();
            for (Map.Entry<String, Object> entry : nodeMetricsMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metricData = (Map<String, Object>) entry.getValue();
                    metrics.put(entry.getKey(), mapToNodeMetrics(entry.getKey(), metricData));
                }
            }
            response.setNodeMetrics(metrics);
        }
        
        return response;
    }

    @Override
    public NodeOutDTO debugNode(String workflowId, String nodeId, Map<String, Object> inputs) {
        log.info("Debugging node: {} in workflow: {}", nodeId, workflowId);
        
        WorkflowDTO workflow = getWorkflowById(workflowId);
        return executeNode(workflow, nodeId, inputs);
    }

    @Override
    public void executeWorkflowAsync(String workflowId, Map<String, Object> inputs, BiConsumer<Map<String, Object>, Throwable> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return executeWorkflow(workflowId, inputs);
            } catch (Exception e) {
                log.error("Error in async workflow execution", e);
                throw e;
            }
        }).whenComplete((result, error) -> callback.accept(result, error));
    }

    // 内部工具方法

    private EdgeStatusDTO mapToEdgeStatus(Map<String, Object> edgeData) {
        String statusStr = (String) edgeData.getOrDefault("status", "WAITING");
        EdgeStatusDTO.EdgeStatusEnum status;
        try {
            status = EdgeStatusDTO.EdgeStatusEnum.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            status = EdgeStatusDTO.EdgeStatusEnum.WAITING;
        }
        
        return EdgeStatusDTO.builder()
                .id((String) edgeData.get("id"))
                .sourceNodeId((String) edgeData.get("sourceNodeId"))
                .targetNodeId((String) edgeData.get("targetNodeId"))
                .status(status)
                .activatedAt(getLongFromMap(edgeData, "activatedAt", null))
                .build();
    }

    private NodeExecutionMetrics mapToNodeMetrics(String nodeId, Map<String, Object> metricData) {
        return NodeExecutionMetrics.builder()
                .nodeId(nodeId)
                .nodeType((String) metricData.get("nodeType"))
                .executionTimeMs(getLongFromMap(metricData, "executionTimeMs", 0L))
                .inputTokens(getIntFromMap(metricData, "inputTokens", null))
                .outputTokens(getIntFromMap(metricData, "outputTokens", null))
                .totalTokens(getIntFromMap(metricData, "totalTokens", null))
                .cost(getDoubleFromMap(metricData, "cost", null))
                .tokenRate(getDoubleFromMap(metricData, "tokenRate", null))
                .memoryUsageMb(getLongFromMap(metricData, "memoryUsageMb", null))
                .errorMessage((String) metricData.get("errorMessage"))
                .retryCount(getIntFromMap(metricData, "retryCount", null))
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getListFromMap(Map<String, Object> map, String key, Class<?> type) {
        if (map.containsKey(key) && map.get(key) instanceof List) {
            List<?> list = (List<?>) map.get(key);
            if (list.isEmpty() || type.isInstance(list.get(0)) || list.get(0) == null) {
                return (List<T>) list;
            }
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromMap(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) instanceof Map) {
            return (Map<String, Object>) map.get(key);
        }
        return new HashMap<>();
    }

    private Integer getIntFromMap(Map<String, Object> map, String key, Integer defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private Long getLongFromMap(Map<String, Object> map, String key, Long defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private Double getDoubleFromMap(Map<String, Object> map, String key, Double defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    // 内部类：流式工作流执行器
    private class StreamingWorkflowExecutor {
        private final WorkflowDTO workflow;
        private final Map<String, Object> context;
        private final BiConsumer<String, Boolean> chunkConsumer;
        private final WorkflowMonitorService monitorService;
        private final String executionId;
        
        private final Set<String> visitedNodes = new HashSet<>();
        private final Map<String, Object> outputs = new HashMap<>();
        private final List<EdgeStatusDTO> edgeStatuses = new ArrayList<>();
        private final Map<String, NodeExecutionMetrics> nodeMetrics = new HashMap<>();
        
        private Map<String, NodeDefDTO> nodeMap;
        private Map<String, List<ConnectionDTO>> targetConnectionMap;
        private Map<String, List<ConnectionDTO>> sourceConnectionMap;
        
        // 记录节点处理中的中间结果
        private final StringBuilder currentChunk = new StringBuilder();
        private long lastChunkSentTime = 0;
        private static final long MIN_CHUNK_INTERVAL_MS = 100; // 最小块发送间隔(ms)
        
        public StreamingWorkflowExecutor(
                WorkflowDTO workflow, 
                Map<String, Object> context, 
                BiConsumer<String, Boolean> chunkConsumer,
                WorkflowMonitorService monitorService,
                String executionId) {
            this.workflow = workflow;
            this.context = context;
            this.chunkConsumer = chunkConsumer;
            this.monitorService = monitorService;
            this.executionId = executionId;
            
            // 初始化节点和连接映射
            initializeMaps();
        }
        
        public void execute() {
            try {
                // 查找入口节点
                List<NodeDefDTO> entryNodes = workflow.getNodes().stream()
                        .filter(node -> Boolean.TRUE.equals(node.getIsEntry()))
                        .collect(Collectors.toList());
                
                if (entryNodes.isEmpty()) {
                    throw new WorkflowExecutionException("No entry nodes found in workflow");
                }
                
                // 记录初始状态
                recordWorkflowState();
                
                // 执行每个入口节点
                for (NodeDefDTO entryNode : entryNodes) {
                    Map<String, Object> nodeResults = executeNodeAndDownstreamStreaming(
                            entryNode.getId(),
                            context,
                            visitedNodes);
                    
                    if (nodeResults != null && !nodeResults.isEmpty()) {
                        outputs.putAll(nodeResults);
                    }
                }
                
                // 发送最后的数据块
                flushCurrentChunk(true);
                
            } catch (Exception e) {
                log.error("Error in streaming workflow execution", e);
                sendErrorChunk(e.getMessage());
            }
        }
        
        private void initializeMaps() {
            // 创建节点映射
            nodeMap = workflow.getNodes().stream()
                    .filter(n -> n != null && n.getId() != null)
                    .collect(Collectors.toMap(
                            NodeDefDTO::getId,
                            Function.identity(),
                            (a, b) -> a
                    ));
            
            // 创建连接映射
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
        }
        
        private Map<String, Object> executeNodeAndDownstreamStreaming(
                String nodeId,
                Map<String, Object> executionContext,
                Set<String> visitedNodes) {
            
            // 防止循环执行
            if (visitedNodes.contains(nodeId)) {
                log.debug("Node already visited, skipping: {}", nodeId);
                return Collections.emptyMap();
            }
            
            // 检查节点是否存在
            if (!nodeMap.containsKey(nodeId)) {
                log.warn("Node not found: {}", nodeId);
                return Collections.emptyMap();
            }
            
            NodeDefDTO node = nodeMap.get(nodeId);
            long nodeStartTime = System.currentTimeMillis();
            
            // 标记节点为已访问
            visitedNodes.add(nodeId);
            
            try {
                // 处理节点输入参数
                Map<String, Object> nodeInputs = prepareNodeInputs(node, executionContext);
                
                // 执行节点
                NodeOutDTO nodeOut = executeNodeWithTimeout(node, nodeInputs);
                
                // 记录节点执行指标
                recordNodeMetrics(node, nodeStartTime, nodeOut);
                
                // 将节点输出添加到执行上下文
                if (nodeOut.getOutputs() != null) {
                    executionContext.putAll(nodeOut.getOutputs());
                    
                    // 检查是否有需要发送的文本块
                    checkAndSendChunk(nodeOut);
                }
                
                // 获取并执行下游节点
                List<ConnectionDTO> outgoingConnections = sourceConnectionMap.getOrDefault(nodeId, Collections.emptyList());
                Map<String, Object> downstreamOutputs = new HashMap<>();
                
                for (ConnectionDTO connection : outgoingConnections) {
                    String targetNodeId = connection.getTargetNodeId();
                    
                    // 设置边缘状态为活动
                    updateEdgeStatus(connection.getId(), EdgeStatusDTO.EdgeStatusEnum.ACTIVE);
                    
                    // 根据连接条件确定是否执行目标节点
                    boolean shouldExecuteTarget = evaluateConnectionCondition(connection, executionContext);
                    
                    if (shouldExecuteTarget) {
                        // 递归执行目标节点
                        Map<String, Object> targetOutputs = executeNodeAndDownstreamStreaming(
                                targetNodeId,
                                executionContext,
                                visitedNodes);
                        
                        if (targetOutputs != null && !targetOutputs.isEmpty()) {
                            downstreamOutputs.putAll(targetOutputs);
                        }
                    } else {
                        // 标记为跳过
                        updateEdgeStatus(connection.getId(), EdgeStatusDTO.EdgeStatusEnum.SKIPPED);
                    }
                }
                
                // 合并当前节点和下游节点的输出
                Map<String, Object> combinedOutputs = new HashMap<>(nodeOut.getOutputs());
                combinedOutputs.putAll(downstreamOutputs);
                
                // 更新工作流状态
                recordWorkflowState();
                
                return combinedOutputs;
                
            } catch (Exception e) {
                log.error("Error executing node: {}", nodeId, e);
                recordNodeError(node, e);
                
                // 发送错误消息
                sendErrorChunk("Error in node " + node.getName() + ": " + e.getMessage());
                
                throw new WorkflowExecutionException("Error executing node: " + nodeId, e);
            }
        }
        
        private Map<String, Object> prepareNodeInputs(NodeDefDTO node, Map<String, Object> executionContext) {
            Map<String, Object> nodeInputs = new HashMap<>();
            
            // 从节点定义和上下文中提取输入参数
            if (node.getInputs() != null) {
                for (NodeInputDTO input : node.getInputs()) {
                    String inputKey = input.getKey();
                    String mappedKey = input.getMappedKey();
                    
                    if (mappedKey != null && executionContext.containsKey(mappedKey)) {
                        // 使用映射的键从上下文中获取值
                        nodeInputs.put(inputKey, executionContext.get(mappedKey));
                    } else if (input.getValue() != null) {
                        // 使用节点定义中的默认值
                        nodeInputs.put(inputKey, input.getValue());
                    } else if (executionContext.containsKey(inputKey)) {
                        // 直接从上下文中获取值
                        nodeInputs.put(inputKey, executionContext.get(inputKey));
                    }
                }
            }
            
            return nodeInputs;
        }
        
        private NodeOutDTO executeNodeWithTimeout(NodeDefDTO node, Map<String, Object> nodeInputs) {
            // 这里可以添加超时逻辑
            return handleNodeExecution(node, nodeInputs);
        }
        
        private boolean evaluateConnectionCondition(ConnectionDTO connection, Map<String, Object> context) {
            // 暂时简单实现：如果没有条件，则总是执行
            String condition = connection.getCondition();
            if (condition == null || condition.trim().isEmpty()) {
                return true;
            }
            
            // 这里可以添加条件表达式解析和计算
            // 例如使用Spring的ExpressionParser
            
            return true; // 默认执行
        }
        
        private void updateEdgeStatus(String edgeId, EdgeStatusDTO.EdgeStatusEnum status) {
            // 查找现有边缘状态或创建新的
            EdgeStatusDTO edgeStatus = edgeStatuses.stream()
                    .filter(e -> e.getId().equals(edgeId))
                    .findFirst()
                    .orElse(null);
            
            if (edgeStatus == null) {
                edgeStatus = EdgeStatusDTO.builder()
                        .id(edgeId)
                        .status(status)
                        .build();
                edgeStatuses.add(edgeStatus);
            } else {
                edgeStatus.setStatus(status);
            }
            
            if (status == EdgeStatusDTO.EdgeStatusEnum.ACTIVE) {
                edgeStatus.setActivatedAt(System.currentTimeMillis());
            }
        }
        
        private void sendChunk(String chunk) {
            if (!completed) {
                chunkConsumer.accept(chunk, false);
            }
        }
        
        public void complete() {
            if (!completed) {
                completed = true;
                chunkConsumer.accept("", true);
            }
        }
        
        public boolean isCompleted() {
            return completed;
        }
    }

    /**
     * Handle text editor node
     */
    private NodeOutDTO handleTextEditorNode(NodeExecutionContext context) {
        log.info("Executing text editor node: {}", context.getNodeId());
        
        try {
            // Get the input text
            String inputText = getRequiredInput(context, "text", String.class);
            
            // Get editor operations
            Map<String, Object> operations = context.getNodeProperties().getOrDefault("operations", Collections.emptyMap());
            
            // Apply text operations
            String processedText = applyTextOperations(inputText, operations);
            
            // Create result
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", processedText);
            
            // Log success
            log.info("Text editor node {} processed text successfully", context.getNodeId());
            
            // Record execution metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("textLength", processedText.length());
            metrics.put("operationsApplied", operations.size());
            
            return createSuccessResult(context, outputs, metrics);
        } catch (Exception e) {
            log.error("Error in text editor node {}: {}", context.getNodeId(), e.getMessage());
            return createErrorResult(context, e.getMessage());
        }
    }
    
    /**
     * Apply text operations to the input text
     */
    private String applyTextOperations(String text, Map<String, Object> operations) {
        if (operations == null || operations.isEmpty()) {
            return text;
        }
        
        String result = text;
        
        // Apply trim if specified
        if (Boolean.TRUE.equals(operations.get("trim"))) {
            result = result.trim();
        }
        
        // Apply case transformation
        String caseOperation = (String) operations.get("case");
        if (caseOperation != null) {
            switch (caseOperation.toLowerCase()) {
                case "upper":
                    result = result.toUpperCase();
                    break;
                case "lower":
                    result = result.toLowerCase();
                    break;
                case "title":
                    // Simple title case implementation
                    if (!result.isEmpty()) {
                        char[] chars = result.toCharArray();
                        boolean capitalizeNext = true;
                        
                        for (int i = 0; i < chars.length; i++) {
                            if (Character.isWhitespace(chars[i])) {
                                capitalizeNext = true;
                            } else if (capitalizeNext) {
                                chars[i] = Character.toUpperCase(chars[i]);
                                capitalizeNext = false;
                            }
                        }
                        
                        result = new String(chars);
                    }
                    break;
            }
        }
        
        // Apply replace operations
        @SuppressWarnings("unchecked")
        List<Map<String, String>> replaceOperations = (List<Map<String, String>>) operations.get("replace");
        if (replaceOperations != null) {
            for (Map<String, String> replace : replaceOperations) {
                String pattern = replace.get("pattern");
                String replacement = replace.get("replacement");
                if (pattern != null && replacement != null) {
                    result = result.replace(pattern, replacement);
                }
            }
        }
        
        // Apply prefix if specified
        String prefix = (String) operations.get("prefix");
        if (prefix != null) {
            result = prefix + result;
        }
        
        // Apply suffix if specified
        String suffix = (String) operations.get("suffix");
        if (suffix != null) {
            result = result + suffix;
        }
        
        return result;
    }
    
    /**
     * Handle custom feedback node
     */
    private NodeOutDTO handleCustomFeedbackNode(NodeExecutionContext context) {
        log.info("Executing custom feedback node: {}", context.getNodeId());
        
        try {
            // Get required inputs
            String message = getRequiredInput(context, "message", String.class);
            
            // Get optional inputs
            String title = getOptionalInput(context, "title", String.class, "Feedback Required");
            String type = getOptionalInput(context, "type", String.class, "text");
            Integer timeoutSeconds = getOptionalInput(context, "timeout", Integer.class, 300);
            
            // Create interaction state
            Map<String, Object> interactionContext = new HashMap<>(context.getVariables());
            
            // Add node-specific data
            Map<String, Object> interactionData = new HashMap<>();
            interactionData.put("message", message);
            interactionData.put("type", type);
            
            // Determine interaction type
            InteractionTypeEnum interactionType = InteractionTypeEnum.CUSTOM_FEEDBACK;
            
            // Pause workflow for interaction
            WorkflowInteractionState state = interactionService.pauseForInteraction(
                context.getExecutionId(),
                context.getWorkflowId(),
                context.getNodeId(),
                interactionType,
                interactionData,
                interactionContext,
                timeoutSeconds * 1000L, // Convert to milliseconds
                message,
                title
            );
            
            // Create a suspended result (will be resumed later)
            return createSuspendedResult(context, state);
        } catch (Exception e) {
            log.error("Error in custom feedback node {}: {}", context.getNodeId(), e.getMessage());
            return createErrorResult(context, e.getMessage());
        }
    }

    /**
     * Create a successful result for a node
     */
    private NodeOutDTO createSuccessResult(NodeExecutionContext context, Map<String, Object> outputs, Map<String, Object> metrics) {
        NodeOutDTO result = new NodeOutDTO();
        result.setSuccess(true);
        result.setNodeId(context.getNodeId());
        result.setOutputs(outputs);
        result.setMetadata(metrics);
        return result;
    }

    /**
     * Create an error result for a node
     */
    private NodeOutDTO createErrorResult(NodeExecutionContext context, String errorMessage) {
        NodeOutDTO result = new NodeOutDTO();
        result.setSuccess(false);
        result.setNodeId(context.getNodeId());
        result.setError(errorMessage);
        return result;
    }

    /**
     * Create a suspended result for a node
     */
    private NodeOutDTO createSuspendedResult(NodeExecutionContext context, WorkflowInteractionState state) {
        NodeOutDTO result = new NodeOutDTO();
        result.setSuccess(true);
        result.setNodeId(context.getNodeId());
        result.setSuspended(true);
        result.setInteractionId(state.getInteractionId());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("interactionType", state.getInteractionType().getValue());
        metadata.put("timeoutMs", state.getTimeoutMs());
        
        result.setMetadata(metadata);
        return result;
    }
    
    /**
     * Get a required input value with type conversion
     */
    private <T> T getRequiredInput(NodeExecutionContext context, String inputName, Class<T> type) {
        Object value = context.getInputs().get(inputName);
        if (value == null) {
            throw new IllegalArgumentException("Required input '" + inputName + "' is missing");
        }
        
        return convertValue(value, type);
    }
    
    /**
     * Get an optional input value with type conversion and default value
     */
    private <T> T getOptionalInput(NodeExecutionContext context, String inputName, Class<T> type, T defaultValue) {
        Object value = context.getInputs().get(inputName);
        if (value == null) {
            return defaultValue;
        }
        
        return convertValue(value, type);
    }
    
    /**
     * Convert a value to the specified type
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        if (type == String.class) {
            return (T) value.toString();
        }
        
        if (type == Integer.class) {
            if (value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            }
            try {
                return (T) Integer.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert value to Integer: " + value);
            }
        }
        
        if (type == Double.class) {
            if (value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            }
            try {
                return (T) Double.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert value to Double: " + value);
            }
        }
        
        if (type == Boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            }
            return (T) Boolean.valueOf(value.toString());
        }
        
        throw new IllegalArgumentException("Cannot convert value to type " + type.getName() + ": " + value);
    }

    /**
     * Resume a workflow execution with updated context
     */
    @Override
    public Map<String, Object> resumeExecution(String executionId, Map<String, Object> context) {
        log.info("Resuming workflow execution: {}", executionId);
        
        // Validate executionId
        if (executionId == null || executionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Execution ID cannot be empty");
        }
        
        // Get interaction state to retrieve workflow ID and node ID
        Optional<WorkflowInteractionState> stateOpt = interactionService.getInteractionState(executionId);
        if (!stateOpt.isPresent()) {
            throw new ResourceNotFoundException("Workflow Execution", "id", executionId);
        }
        
        WorkflowInteractionState state = stateOpt.get();
        String workflowId = state.getWorkflowId();
        String currentNodeId = state.getCurrentNodeId();
        
        log.info("Resuming workflow {} at node {}", workflowId, currentNodeId);
        
        // Get the workflow
        WorkflowDTO workflow = getWorkflowById(workflowId);
        if (workflow == null) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }
        
        // Mark that we're resuming from an interaction
        context.put("__resuming_from_interaction", true);
        context.put("__execution_id", executionId);
        context.put("__workflow_id", workflowId);
        context.put("__current_node_id", currentNodeId);
        context.put("__start_time", System.currentTimeMillis());
        
        try {
            // Resume from the current node
            Map<String, Object> result = dispatchWorkflow(workflow, context, currentNodeId);
            
            log.info("Successfully resumed and completed workflow execution: {}", executionId);
            return result;
        } catch (Exception e) {
            log.error("Error resuming workflow execution: {}", e.getMessage(), e);
            
            // Record failure in monitoring service
            monitorService.failExecution(executionId, e.getMessage());
            
            throw new WorkflowExecutionException("Failed to resume workflow execution: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> executeWorkflowWithStream(String workflowId, Map<String, Object> inputs, 
                                                        Consumer<Map<String, Object>> progressConsumer) {
        log.info("Executing workflow with stream: {}, inputs: {}", workflowId, inputs);
        
        // Initialize result map with inputs
        Map<String, Object> result = new HashMap<>(inputs);
        
        try {
            // Dispatch the workflow with progress updates
            dispatchWorkflow(workflowId, result, (nodeId, nodeResult) -> {
                // Create a progress update with current result state
                Map<String, Object> progressUpdate = new HashMap<>(result);
                progressUpdate.put("currentNodeId", nodeId);
                progressUpdate.put("currentNodeResult", nodeResult);
                
                // Send progress update through the consumer
                progressConsumer.accept(progressUpdate);
            });
            
            // Return the final result after workflow completion
            return result;
        } catch (Exception e) {
            log.error("Error executing workflow with stream: {}", workflowId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            progressConsumer.accept(errorResult);
            throw e;
        }
    }
} 