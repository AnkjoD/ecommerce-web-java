package com.ankkun.ecommerce.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String message;

    @JsonProperty("chat_history")
    private List<Object> chatHistory = List.of();
}
