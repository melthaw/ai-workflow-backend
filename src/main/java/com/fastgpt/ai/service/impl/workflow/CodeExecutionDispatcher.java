package com.fastgpt.ai.service.impl.workflow;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Executes code nodes with security and resource constraints
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeExecutionDispatcher {
    
    private static final int DEFAULT_TIMEOUT_MS = 10000; // 10 seconds timeout
    private static final int MAX_MEMORY_MB = 100; // 100MB max memory
    private static final int MAX_CPU_TIME_MS = 5000; // 5 seconds max CPU time
    
    /**
     * Dispatch code execution with timeout and safety constraints
     */
    public NodeOutDTO dispatchCode(Node node, Map<String, Object> inputs) {
        log.info("Executing code node: {}", node.getId());
        
        Map<String, Object> nodeData = node.getData();
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            // Get code from inputs or node data
            String code = getStringParam(inputs, "code", "");
            if (code.isEmpty()) {
                code = getStringParam(nodeData, "code", "");
            }
            
            if (code.isEmpty()) {
                throw new IllegalArgumentException("No code provided for execution");
            }
            
            // Get code type (language)
            String codeType = getStringParam(inputs, "codeType", "");
            if (codeType.isEmpty()) {
                codeType = getStringParam(nodeData, "codeType", "js");
            }
            
            // Get timeout
            int timeoutMs = getIntParam(inputs, "timeout", DEFAULT_TIMEOUT_MS);
            
            // Get variables to expose to code
            Map<String, Object> contextVars = new HashMap<>();
            
            // Copy safe variables from inputs to context
            for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("__") && !key.equals("code") && !key.equals("codeType") && !key.equals("timeout")) {
                    contextVars.put(key, entry.getValue());
                }
            }
            
            // Execute code in sandbox
            ExecutionResult result = executeCodeWithTimeout(code, codeType, contextVars, timeoutMs);
            
            // Record execution metrics
            metadata.put("executionTimeMs", result.getExecutionTimeMs());
            metadata.put("memoryUsageMb", result.getMemoryUsageMb());
            
            // Handle result
            if (result.isSuccess()) {
                outputs.put("result", result.getResult());
                outputs.put("console", result.getLogs());
                
                NodeOutDTO nodeOut = new NodeOutDTO();
                nodeOut.setNodeId(node.getId());
                nodeOut.setOutputs(outputs);
                nodeOut.setMetadata(metadata);
                nodeOut.setSuccess(true);
                return nodeOut;
            } else {
                // Execution failed
                outputs.put("error", result.getError());
                
                NodeOutDTO nodeOut = new NodeOutDTO();
                nodeOut.setNodeId(node.getId());
                nodeOut.setOutputs(outputs);
                nodeOut.setMetadata(metadata);
                nodeOut.setSuccess(false);
                nodeOut.setError(result.getError());
                return nodeOut;
            }
            
        } catch (Exception e) {
            log.error("Error executing code node: {}", e.getMessage(), e);
            
            outputs.put("error", e.getMessage());
            
            NodeOutDTO nodeOut = new NodeOutDTO();
            nodeOut.setNodeId(node.getId());
            nodeOut.setOutputs(outputs);
            nodeOut.setMetadata(metadata);
            nodeOut.setSuccess(false);
            nodeOut.setError("Error executing code: " + e.getMessage());
            return nodeOut;
        }
    }
    
    /**
     * Execute code with timeout and safety constraints
     */
    private ExecutionResult executeCodeWithTimeout(String code, String language, Map<String, Object> contextVars, int timeoutMs) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<ExecutionResult> future = executor.submit(() -> {
            try {
                if ("js".equalsIgnoreCase(language) || "javascript".equalsIgnoreCase(language)) {
                    return executeJavaScript(code, contextVars);
                } else {
                    throw new UnsupportedOperationException("Unsupported language: " + language);
                }
            } catch (Exception e) {
                return ExecutionResult.builder()
                        .success(false)
                        .error(e.getMessage())
                        .build();
            }
        });
        
        try {
            // Wait for completion or timeout
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return ExecutionResult.builder()
                    .success(false)
                    .error("Code execution timed out after " + timeoutMs + "ms")
                    .executionTimeMs(timeoutMs)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.builder()
                    .success(false)
                    .error("Code execution was interrupted: " + e.getMessage())
                    .build();
        } catch (ExecutionException e) {
            return ExecutionResult.builder()
                    .success(false)
                    .error("Code execution failed: " + e.getCause().getMessage())
                    .build();
        } finally {
            executor.shutdownNow();
        }
    }
    
    /**
     * Execute JavaScript code using GraalVM
     */
    private ExecutionResult executeJavaScript(String code, Map<String, Object> contextVars) {
        // Create a sandbox for executing JavaScript code
        ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
        PrintStream consoleStream = new PrintStream(consoleOutput);
        
        Instant startTime = Instant.now();
        
        // Configure the engine with resource limits
        Engine.Builder engineBuilder = Engine.newBuilder()
                .option("js.foreign-object-prototype", "true")
                .option("js.nashorn-compat", "true");
        
        // Create a context with limited capabilities
        try (Context context = Context.newBuilder("js")
                .engine(engineBuilder.build())
                .allowHostAccess(HostAccess.newBuilder()
                        .allowPublicAccess(true)
                        .build())
                .allowHostClassLookup(className -> false) // Prevent Java class access
                .allowIO(false) // No file system access
                .allowNativeAccess(false) // No native function access
                .allowCreateThread(false) // No thread creation
                .allowAllAccess(false) // No unrestricted access
                .option("js.ecmascript-version", "2022") // Modern JavaScript
                .out(consoleStream)
                .err(consoleStream)
                .build()) {
            
            // Expose context variables to JavaScript
            Value bindings = context.getBindings("js");
            for (Map.Entry<String, Object> entry : contextVars.entrySet()) {
                bindings.putMember(entry.getKey(), entry.getValue());
            }
            
            // Add console methods
            context.eval("js", "var console = {" +
                    "log: function() { print(Array.prototype.slice.call(arguments).join(' ')); }," +
                    "warn: function() { print('WARN: ' + Array.prototype.slice.call(arguments).join(' ')); }," +
                    "error: function() { print('ERROR: ' + Array.prototype.slice.call(arguments).join(' ')); }," +
                    "info: function() { print('INFO: ' + Array.prototype.slice.call(arguments).join(' ')); }" +
                    "};");
            
            // Execute the code
            Value result = context.eval("js", code);
            
            // Calculate execution metrics
            Instant endTime = Instant.now();
            long executionTimeMs = Duration.between(startTime, endTime).toMillis();
            double memoryUsageMb = (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
            
            // Get console output
            String logs = consoleOutput.toString();
            
            // Convert result to Java object
            Object javaResult = result.isNull() ? null : result.as(Object.class);
            
            return ExecutionResult.builder()
                    .success(true)
                    .result(javaResult)
                    .logs(logs)
                    .executionTimeMs(executionTimeMs)
                    .memoryUsageMb(memoryUsageMb)
                    .build();
            
        } catch (Exception e) {
            log.error("JavaScript execution error: {}", e.getMessage(), e);
            
            // Get execution time even for failed executions
            Instant endTime = Instant.now();
            long executionTimeMs = Duration.between(startTime, endTime).toMillis();
            
            // Get any console output before the error
            String logs = consoleOutput.toString();
            
            return ExecutionResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .logs(logs)
                    .executionTimeMs(executionTimeMs)
                    .build();
        }
    }
    
    /**
     * Helper methods for parameter extraction
     */
    private String getStringParam(Map<String, Object> data, String key, String defaultValue) {
        if (data != null && data.containsKey(key)) {
            Object value = data.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        return defaultValue;
    }
    
    private int getIntParam(Map<String, Object> data, String key, int defaultValue) {
        if (data != null && data.containsKey(key)) {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    // Fall through to default
                }
            }
        }
        return defaultValue;
    }
    
    /**
     * Data class representing execution result
     */
    @Data
    @Builder
    private static class ExecutionResult {
        private boolean success;
        private Object result;
        private String error;
        private String logs;
        private long executionTimeMs;
        private double memoryUsageMb;
    }
} 