package com.fastgpt.ai.dto.openai;

/**
 * Represents the token usage of an API call
 */
public class Usage {
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    public Usage() {
    }

    // Getters and setters
    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
} 