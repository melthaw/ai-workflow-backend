package com.fastgpt.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchRequest {
    @NotBlank(message = "KB ID cannot be empty")
    private String kbId;
    
    @NotBlank(message = "Query cannot be empty")
    private String query;
    
    @Min(value = 1, message = "Limit must be at least 1")
    private Integer limit = 10;
    
    private Double minScore = 0.5;
    
    private List<String> filterIds;
    
    private Map<String, List<String>> metadataFilters;
    
    private Boolean useRawQuery = false;
} 