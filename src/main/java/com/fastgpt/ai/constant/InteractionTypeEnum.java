package com.fastgpt.ai.constant;

/**
 * Enumeration of interaction types for workflow nodes
 */
public enum InteractionTypeEnum {
    /**
     * User selection from a list of options
     */
    USER_SELECT("userSelect"),
    
    /**
     * Form input with multiple fields
     */
    FORM_INPUT("formInput"),
    
    /**
     * Single text input
     */
    TEXT_INPUT("textInput"),
    
    /**
     * Confirm/deny decision
     */
    CONFIRMATION("confirmation"),
    
    /**
     * File upload
     */
    FILE_UPLOAD("fileUpload"),
    
    /**
     * Wait for custom feedback
     */
    CUSTOM_FEEDBACK("customFeedback");
    
    private final String value;
    
    InteractionTypeEnum(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static InteractionTypeEnum fromValue(String value) {
        for (InteractionTypeEnum type : InteractionTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + value);
    }
} 