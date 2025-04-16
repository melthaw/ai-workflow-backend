package com.fastgpt.ai.service.function;

import com.fastgpt.ai.dto.function.FunctionCallResponse.FunctionCall;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for executing function calls
 */
@FunctionalInterface
public interface FunctionExecutor {
    
    /**
     * Execute a function with the provided arguments
     * 
     * @param functionCall The function call details
     * @return Future containing the function execution result
     */
    CompletableFuture<Object> execute(FunctionCall functionCall);
    
    /**
     * Create a simple function executor from a lambda
     * 
     * @param executor Lambda function that takes arguments and returns a result
     * @return FunctionExecutor implementation
     */
    static FunctionExecutor of(FunctionLambda executor) {
        return functionCall -> CompletableFuture.completedFuture(
            executor.execute(functionCall.getArguments())
        );
    }
    
    /**
     * Create an async function executor from a lambda
     * 
     * @param executor Async lambda function
     * @return FunctionExecutor implementation
     */
    static FunctionExecutor ofAsync(AsyncFunctionLambda executor) {
        return functionCall -> executor.execute(functionCall.getArguments());
    }
    
    /**
     * Functional interface for synchronous function execution
     */
    @FunctionalInterface
    interface FunctionLambda {
        Object execute(Map<String, Object> arguments);
    }
    
    /**
     * Functional interface for asynchronous function execution
     */
    @FunctionalInterface
    interface AsyncFunctionLambda {
        CompletableFuture<Object> execute(Map<String, Object> arguments);
    }
} 