package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String message;

    private String fullMessage;

    private String model;

    private long timestamp;

    private Long processingTimeMs;

    @Builder.Default
    private boolean error = false;

    private String errorMessage;

    @JsonProperty("streaming")
    @Builder.Default
    private boolean isStreaming = false;

    @JsonProperty("complete")
    @Builder.Default
    private boolean isComplete = false;

    private String conversationId;

    private int tokenCount;

    private String usage;
}
