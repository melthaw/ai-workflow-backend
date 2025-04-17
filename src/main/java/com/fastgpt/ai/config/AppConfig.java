package com.fastgpt.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 应用配置类
 */
@Configuration
public class AppConfig {
    
    /**
     * 配置RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30秒连接超时
        factory.setReadTimeout(30000);    // 30秒读取超时
        
        return new RestTemplateBuilder()
                .requestFactory(() -> new BufferingClientHttpRequestFactory(factory))
                .setConnectTimeout(Duration.ofMillis(30000))
                .setReadTimeout(Duration.ofMillis(30000))
                .build();
    }
    
    /**
     * 配置ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 可以根据需要添加配置
        return objectMapper;
    }
} 