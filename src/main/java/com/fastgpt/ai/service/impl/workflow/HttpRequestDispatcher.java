package com.fastgpt.ai.service.impl.workflow;

import com.fastgpt.ai.dto.workflow.NodeOutDTO;
import com.fastgpt.ai.entity.workflow.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dispatcher for HTTP request workflow node
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRequestDispatcher {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Maximum response size to avoid memory issues
    private static final int MAX_RESPONSE_SIZE = 1024 * 1024; // 1MB
    
    // Pattern for variable interpolation in strings
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^{}]+)\\}\\}");

    /**
     * Execute an HTTP request as defined by the node configuration
     *
     * @param node The node definition
     * @param inputs Input values
     * @return Node execution result
     */
    public NodeOutDTO dispatchHttpRequest(Node node, Map<String, Object> inputs) {
        log.info("Processing HTTP request node: {}", node.getId());
        
        Map<String, Object> nodeData = node.getData() != null ? node.getData() : Collections.emptyMap();
        Map<String, Object> outputs = new HashMap<>();
        
        try {
            // Extract request parameters from node data
            String url = getString(nodeData, "url", "");
            String method = getString(nodeData, "method", "GET");
            String contentType = getString(nodeData, "contentType", "application/json");
            Object bodyObj = nodeData.get("body");
            Object headersObj = nodeData.get("headers");
            Object paramsObj = nodeData.get("params");
            int timeout = getInteger(nodeData, "timeout", 30000);
            
            // Replace variables in URL
            url = interpolateVariables(url, inputs);
            
            // Prepare headers
            HttpHeaders headers = prepareHeaders(headersObj, contentType, inputs);
            
            // Prepare request entity based on content type and method
            HttpEntity<?> requestEntity = prepareRequestEntity(method, contentType, bodyObj, headers, paramsObj, inputs);
            
            // Execute the HTTP request
            ResponseEntity<String> response = executeRequest(url, method, requestEntity, timeout);
            
            // Process the response
            Map<String, Object> responseData = processResponse(response, contentType);
            
            // Set outputs
            outputs.put("response", responseData);
            outputs.put("statusCode", response.getStatusCodeValue());
            outputs.put("success", response.getStatusCode().is2xxSuccessful());
            
            NodeOutDTO result = new NodeOutDTO();
            result.setNodeId(node.getNodeId());
            result.setOutputs(outputs);
            result.setSuccess(true);
            return result;
            
        } catch (Exception e) {
            log.error("Error executing HTTP request: {}", e.getMessage(), e);
            return createErrorResult(node, "HTTP request failed: " + e.getMessage());
        }
    }
    
    /**
     * Prepare HTTP headers
     */
    private HttpHeaders prepareHeaders(Object headersObj, String contentType, Map<String, Object> inputs) {
        HttpHeaders headers = new HttpHeaders();
        
        // Set content type
        if (contentType != null && !contentType.isEmpty()) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        }
        
        // Set additional headers from configuration
        if (headersObj instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> headersMap = (Map<String, String>) headersObj;
                
                headersMap.forEach((key, value) -> {
                    String interpolatedValue = interpolateVariables(value, inputs);
                    headers.add(key, interpolatedValue);
                });
            } catch (ClassCastException e) {
                log.warn("Headers not in expected format", e);
            }
        }
        
        return headers;
    }
    
    /**
     * Prepare the request entity based on content type and method
     */
    private HttpEntity<?> prepareRequestEntity(
            String method, 
            String contentType, 
            Object bodyObj, 
            HttpHeaders headers,
            Object paramsObj,
            Map<String, Object> inputs) throws JsonProcessingException {
        
        // For GET, DELETE and HEAD requests without body
        if ("GET".equalsIgnoreCase(method) || 
            "DELETE".equalsIgnoreCase(method) || 
            "HEAD".equalsIgnoreCase(method)) {
            return new HttpEntity<>(headers);
        }
        
        // For form submissions
        if (contentType.contains("application/x-www-form-urlencoded")) {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            
            if (paramsObj instanceof Map) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramsMap = (Map<String, Object>) paramsObj;
                    
                    paramsMap.forEach((key, value) -> {
                        if (value != null) {
                            formData.add(key, interpolateVariables(value.toString(), inputs));
                        }
                    });
                } catch (ClassCastException e) {
                    log.warn("Form parameters not in expected format", e);
                }
            }
            
            return new HttpEntity<>(formData, headers);
        }
        
        // For multipart/form-data - not fully implemented here
        if (contentType.contains("multipart/form-data")) {
            // Simplified implementation - full implementation would need file handling
            MultiValueMap<String, Object> multipartData = new LinkedMultiValueMap<>();
            
            if (paramsObj instanceof Map) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramsMap = (Map<String, Object>) paramsObj;
                    
                    paramsMap.forEach((key, value) -> {
                        if (value != null) {
                            multipartData.add(key, interpolateVariables(value.toString(), inputs));
                        }
                    });
                } catch (ClassCastException e) {
                    log.warn("Multipart parameters not in expected format", e);
                }
            }
            
            return new HttpEntity<>(multipartData, headers);
        }
        
        // For JSON body
        if (contentType.contains("application/json")) {
            String bodyContent = "";
            
            if (bodyObj instanceof String) {
                bodyContent = interpolateVariables((String) bodyObj, inputs);
            } else if (bodyObj instanceof Map || bodyObj instanceof List) {
                // Process JSON objects/arrays by interpolating string values recursively
                Object processedBody = processJsonBody(bodyObj, inputs);
                bodyContent = objectMapper.writeValueAsString(processedBody);
            }
            
            return new HttpEntity<>(bodyContent, headers);
        }
        
        // For plain text and other types
        if (bodyObj instanceof String) {
            String bodyContent = interpolateVariables((String) bodyObj, inputs);
            return new HttpEntity<>(bodyContent, headers);
        }
        
        // Default: empty body with headers
        return new HttpEntity<>(headers);
    }
    
    /**
     * Process JSON body for variable interpolation
     */
    @SuppressWarnings("unchecked")
    private Object processJsonBody(Object body, Map<String, Object> inputs) {
        if (body instanceof String) {
            return interpolateVariables((String) body, inputs);
        } else if (body instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            ((Map<String, Object>) body).forEach((key, value) -> {
                result.put(key, processJsonBody(value, inputs));
            });
            return result;
        } else if (body instanceof List) {
            List<Object> result = new ArrayList<>();
            ((List<Object>) body).forEach(item -> {
                result.add(processJsonBody(item, inputs));
            });
            return result;
        } else {
            return body; // numbers, booleans, etc.
        }
    }
    
    /**
     * Execute the HTTP request with error handling and timeout
     */
    private ResponseEntity<String> executeRequest(
            String url, 
            String method, 
            HttpEntity<?> requestEntity,
            int timeout) throws RestClientException {
        
        // Convert string method to HttpMethod enum
        HttpMethod httpMethod;
        try {
            httpMethod = HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid HTTP method: " + method);
        }
        
        // Execute the request
        return restTemplate.exchange(
            url,
            httpMethod,
            requestEntity,
            String.class
        );
    }
    
    /**
     * Process the HTTP response
     */
    private Map<String, Object> processResponse(ResponseEntity<String> response, String contentType) {
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.getStatusCodeValue());
        result.put("headers", headersToMap(response.getHeaders()));
        
        // Process response body based on content type
        String responseBody = response.getBody();
        if (responseBody != null) {
            // Truncate response if too large
            if (responseBody.length() > MAX_RESPONSE_SIZE) {
                responseBody = responseBody.substring(0, MAX_RESPONSE_SIZE) + "... [truncated]";
                result.put("truncated", true);
            }
            
            // Parse JSON if response is JSON
            if (contentType.contains("application/json") || 
                (response.getHeaders().getContentType() != null && 
                 response.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON))) {
                try {
                    Object jsonObject = objectMapper.readValue(responseBody, Object.class);
                    result.put("body", jsonObject);
                } catch (Exception e) {
                    log.warn("Failed to parse JSON response, treating as text", e);
                    result.put("body", responseBody);
                }
            } else {
                result.put("body", responseBody);
            }
        }
        
        return result;
    }
    
    /**
     * Convert HttpHeaders to a Map for easier processing
     */
    private Map<String, List<String>> headersToMap(HttpHeaders headers) {
        Map<String, List<String>> result = new HashMap<>();
        headers.forEach(result::put);
        return result;
    }
    
    /**
     * Replace variables in format {{varName}} with values from inputs
     */
    private String interpolateVariables(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = getNestedValue(variables, varName);
            
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Get a nested value from a map using dot notation
     * e.g., "user.name" would get variables.get("user").get("name")
     */
    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;
        
        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Get a string value from the node data with default
     */
    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    /**
     * Get an integer value from the node data with default
     */
    private int getInteger(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Create an error result
     */
    private NodeOutDTO createErrorResult(Node node, String errorMessage) {
        NodeOutDTO result = new NodeOutDTO();
        result.setNodeId(node.getNodeId());
        result.setSuccess(false);
        result.setError(errorMessage);
        return result;
    }
} 