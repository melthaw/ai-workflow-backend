package com.fastgpt.ai.controller;

import com.fastgpt.ai.dto.FunctionCallResult;
import com.fastgpt.ai.dto.FunctionDefinition;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.service.FunctionCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for function registry and execution
 */
@Slf4j
@RestController
@RequestMapping("/api/functions")
@RequiredArgsConstructor
@Tag(name = "Functions", description = "API for managing and executing functions")
public class FunctionController {
    
    private final FunctionCallService functionCallService;
    
    @Operation(summary = "Get all functions", description = "Get all registered functions")
    @GetMapping
    public ResponseEntity<List<FunctionDefinition>> getAllFunctions() {
        return ResponseEntity.ok(functionCallService.getAllFunctions());
    }
    
    @Operation(summary = "Get functions by category", description = "Get functions in a specific category")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<FunctionDefinition>> getFunctionsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(functionCallService.getFunctionsByCategory(category));
    }
    
    @Operation(summary = "Get function by ID", description = "Get a function by its ID")
    @GetMapping("/{functionId}")
    public ResponseEntity<FunctionDefinition> getFunctionById(@PathVariable String functionId) {
        return functionCallService.getFunction(functionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(summary = "Call function by ID", description = "Execute a function by its ID")
    @PostMapping("/{functionId}/call")
    public ResponseEntity<FunctionCallResult> callFunction(
            @PathVariable String functionId,
            @RequestBody Map<String, Object> arguments) {
        try {
            FunctionCallResult result = functionCallService.callFunction(functionId, arguments);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error calling function {}: {}", functionId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    FunctionCallResult.error(functionId, "unknown", arguments, e.getMessage(), 0)
            );
        }
    }
    
    @Operation(summary = "Call function by name", description = "Execute a function by its name")
    @PostMapping("/name/{name}/call")
    public ResponseEntity<FunctionCallResult> callFunctionByName(
            @PathVariable String name,
            @RequestBody Map<String, Object> arguments) {
        try {
            FunctionCallResult result = functionCallService.callFunctionByName(name, arguments);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error calling function {}: {}", name, e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    FunctionCallResult.error("unknown", name, arguments, e.getMessage(), 0)
            );
        }
    }
    
    @Operation(summary = "Register function", description = "Register a new function definition")
    @PostMapping
    public ResponseEntity<FunctionDefinition> registerFunction(@RequestBody FunctionDefinition definition) {
        FunctionDefinition registered = functionCallService.registerFunction(definition);
        return ResponseEntity.ok(registered);
    }
    
    @Operation(summary = "Unregister function", description = "Unregister a function by its ID")
    @DeleteMapping("/{functionId}")
    public ResponseEntity<Map<String, Object>> unregisterFunction(@PathVariable String functionId) {
        boolean removed = functionCallService.unregisterFunction(functionId);
        
        if (removed) {
            return ResponseEntity.ok(Map.of("message", "Function unregistered", "functionId", functionId));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(summary = "Validate function arguments", description = "Validate arguments against a function's schema")
    @PostMapping("/{functionId}/validate")
    public ResponseEntity<Map<String, Object>> validateArguments(
            @PathVariable String functionId,
            @RequestBody Map<String, Object> arguments) {
        
        boolean valid = functionCallService.validateArguments(functionId, arguments);
        
        if (valid) {
            return ResponseEntity.ok(Map.of("valid", true));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "Invalid arguments for function"
            ));
        }
    }
} 