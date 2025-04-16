package com.fastgpt.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous execution
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Configure the async task executor for streaming operations
     * @return Executor for handling async tasks
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("workflow-executor-");
        executor.setKeepAliveSeconds(300); // 5 minutes
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    /**
     * Configure the heartbeat executor for SSE connections
     * @return Executor for handling heartbeats
     */
    @Bean(name = "heartbeatExecutor")
    public Executor heartbeatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("workflow-heartbeat-");
        executor.initialize();
        return executor;
    }
} 