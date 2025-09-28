package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String message;

    @Builder.Default
    private String model = "llama3.1";

    private String conversationId;

    @JsonProperty("streaming")
    @Builder.Default
    private boolean streaming = false;

    private String context;

    @Builder.Default
    private double temperature = 0.7;

    @Builder.Default
    private int maxTokens = 2000;
}
