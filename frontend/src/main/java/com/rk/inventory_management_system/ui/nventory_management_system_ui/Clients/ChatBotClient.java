package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;


import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatBotClient {

    private final RestClient restClient;


    public ChatResponse chat(ChatRequest chatRequest) {
        try {
            log.debug("Sending chat request to AI service: model={}, message length={}",
                    chatRequest.getModel(),
                    chatRequest.getMessage() != null ? chatRequest.getMessage().length() : 0);

            ChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(chatRequest)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null) {
                throw new RestClientException("Received null response from AI service");
            }

            log.debug("Received response from AI service: message length={}",
                    response.getMessage() != null ? response.getMessage().length() : 0);

            return response;

        } catch (RestClientException e) {
            log.error("Failed to communicate with AI service: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during chat request: {}", e.getMessage(), e);
            throw new RestClientException("Failed to process chat request", e);
        }
    }

    public List<String> getAllModels() {
        try {
            log.debug("Fetching available AI models");

            List<String> models = restClient.get()
                    .uri("/api/models")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {});

            if (models == null || models.isEmpty()) {
                log.warn("No models returned from AI service, using defaults");
                return getDefaultModels();
            }

            log.debug("Found {} available models: {}", models.size(), models);
            return models;

        } catch (RestClientException e) {
            log.error("Failed to fetch models from AI service: {}", e.getMessage());
            return getDefaultModels();
        } catch (Exception e) {
            log.error("Unexpected error fetching models: {}", e.getMessage(), e);
            return getDefaultModels();
        }
    }

    private List<String> getDefaultModels() {
        return Arrays.asList("llama3", "mistral", "gemma");
    }
}
