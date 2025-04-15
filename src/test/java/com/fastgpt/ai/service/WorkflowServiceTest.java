package com.fastgpt.ai.service;

import com.fastgpt.ai.dto.workflow.*;
import com.fastgpt.ai.exception.WorkflowExecutionException;
import com.fastgpt.ai.service.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    private WorkflowService workflowService;
    
    @Mock
    private AiService aiService;
    
    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    
    @Mock
    private WorkflowMonitorService monitorService;
    
    @BeforeEach
    void setUp() {
        // 仅测试dispatchWorkflow部分，不需要真正的存储库和Mapper
        workflowService = new WorkflowServiceImpl(null, null, aiService, knowledgeBaseService, monitorService);
        
        // 配置监控服务模拟
        when(monitorService.recordWorkflowStart(anyString(), anyMap())).thenReturn("test-execution-id");
        doNothing().when(monitorService).recordWorkflowComplete(anyString(), anyString(), anyMap(), anyLong(), anyInt());
        doNothing().when(monitorService).recordWorkflowError(anyString(), anyString(), anyString(), anyLong(), anyInt());
        doNothing().when(monitorService).recordNodeStart(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap());
        doNothing().when(monitorService).recordNodeComplete(anyString(), anyString(), anyString(), any(NodeOutDTO.class), anyLong());
    }
    
    @Test
    void testDispatchWorkflow_SimpleInputOutput() {
        // 创建一个简单的输入->输出工作流
        WorkflowDTO workflow = createSimpleWorkflow();
        
        // 准备测试输入
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("message", "Hello, workflow!");
        
        // 执行工作流
        Map<String, Object> result = workflowService.dispatchWorkflow(workflow, inputs, null);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("Hello, workflow!", result.get("message"));
        verify(monitorService).recordWorkflowStart(eq(workflow.getWorkflowId()), anyMap());
        verify(monitorService, atLeastOnce()).recordNodeStart(anyString(), eq(workflow.getWorkflowId()), anyString(), anyString(), anyString(), anyMap());
        verify(monitorService, atLeastOnce()).recordNodeComplete(anyString(), eq(workflow.getWorkflowId()), anyString(), any(NodeOutDTO.class), anyLong());
        verify(monitorService).recordWorkflowComplete(anyString(), eq(workflow.getWorkflowId()), anyMap(), anyLong(), eq(2)); // 两个节点应该被处理
    }
    
    @Test
    void testDispatchWorkflow_NoNodes() {
        // 创建一个没有节点的工作流
        WorkflowDTO workflow = WorkflowDTO.builder()
                .workflowId("test-workflow")
                .nodes(Collections.emptyList())
                .build();
        
        // 执行工作流并期待异常
        WorkflowExecutionException exception = assertThrows(WorkflowExecutionException.class, () -> {
            workflowService.dispatchWorkflow(workflow, Collections.emptyMap(), null);
        });
        
        assertEquals("Workflow execution failed: Workflow has no nodes", exception.getMessage());
        verify(monitorService).recordWorkflowStart(eq(workflow.getWorkflowId()), anyMap());
        verify(monitorService).recordWorkflowError(anyString(), eq(workflow.getWorkflowId()), eq("Workflow has no nodes"), anyLong(), eq(0));
    }
    
    @Test
    void testDispatchWorkflow_NoEntryNodes() {
        // 创建一个有节点但没有入口节点的工作流
        WorkflowDTO workflow = WorkflowDTO.builder()
                .workflowId("test-workflow")
                .nodes(List.of(
                        NodeDefDTO.builder()
                                .id("node1")
                                .type("input")
                                .name("Input Node")
                                .isEntry(false) // 非入口节点
                                .build()
                ))
                .build();
        
        // 执行工作流并期待异常
        WorkflowExecutionException exception = assertThrows(WorkflowExecutionException.class, () -> {
            workflowService.dispatchWorkflow(workflow, Collections.emptyMap(), null);
        });
        
        assertEquals("Workflow execution failed: No entry nodes found in workflow", exception.getMessage());
        verify(monitorService).recordWorkflowStart(eq(workflow.getWorkflowId()), anyMap());
        verify(monitorService).recordWorkflowError(anyString(), eq(workflow.getWorkflowId()), eq("No entry nodes found in workflow"), anyLong(), eq(0));
    }
    
    /**
     * 创建一个简单的输入->输出工作流
     */
    private WorkflowDTO createSimpleWorkflow() {
        // 输入节点
        NodeDefDTO inputNode = NodeDefDTO.builder()
                .id("input-node")
                .type("input")
                .name("Input Node")
                .isEntry(true)
                .inputs(Collections.emptyList())
                .outputs(List.of(
                        ConnectionDTO.builder()
                                .sourceNodeId("input-node")
                                .sourceHandle("message")
                                .targetNodeId("output-node")
                                .targetHandle("message")
                                .build()
                ))
                .data(Collections.emptyMap())
                .build();
        
        // 输出节点
        NodeDefDTO outputNode = NodeDefDTO.builder()
                .id("output-node")
                .type("output")
                .name("Output Node")
                .isEntry(false)
                .inputs(List.of(
                        ConnectionDTO.builder()
                                .sourceNodeId("input-node")
                                .sourceHandle("message")
                                .targetNodeId("output-node")
                                .targetHandle("message")
                                .build()
                ))
                .outputs(Collections.emptyList())
                .data(Collections.emptyMap())
                .build();
        
        // 节点间的连接
        ConnectionDTO connection = ConnectionDTO.builder()
                .sourceNodeId("input-node")
                .sourceHandle("message")
                .targetNodeId("output-node")
                .targetHandle("message")
                .build();
        
        // 构建工作流
        return WorkflowDTO.builder()
                .id("test-flow-id")
                .workflowId("test-workflow")
                .name("Test Workflow")
                .nodes(List.of(inputNode, outputNode))
                .edges(List.of(connection))
                .build();
    }
} 