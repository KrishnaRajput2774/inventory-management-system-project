package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatResponse;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.MessageResponse;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ModelListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatbotClient {

    private final RestClient restClient;
    private final WebClient webClient;

    /**
     * Send a standard chat message (non-streaming)
     */
    public com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatResponse sendMessage(ChatRequest request) {
        log.debug("Sending chat message: {}", request.getMessage());
        return restClient.post()
                .uri("/api/chatbot/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatResponse.class);
    }

    /**
     * Send a streaming chat message using SSE
     */
    public Flux<String> sendMessageStream(ChatRequest request) {
        log.debug("Sending streaming chat message: {}", request.getMessage());

        return webClient.post()
                .uri("/api/chatbot/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(data -> !data.isEmpty())
                .map(data -> {
                    // Parse SSE data format
                    if (data.startsWith("data: ")) {
                        return data.substring(6);
                    }
                    return data;
                })
                .takeWhile(data -> !data.equals("[DONE]"))
                .timeout(Duration.ofMinutes(5))
                .doOnError(error -> log.error("Error in chat stream", error))
                .onErrorResume(error -> Flux.just("Error: " + error.getMessage()));
    }

    /**
     * Send a streaming chat message with JSON responses
     */
    public Flux<ChatResponse> sendMessageStreamJson(ChatRequest request) {
        log.debug("Sending JSON streaming chat message: {}", request.getMessage());

        return webClient.post()
                .uri("/api/chatbot/chat/stream-json")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ChatResponse.class)
                .timeout(Duration.ofMinutes(5))
                .doOnError(error -> log.error("Error in JSON chat stream", error));
    }

    /**
     * Get available AI models
     */
    public ModelListResponse getAvailableModels() {
        log.debug("Fetching available models");
        return restClient.get()
                .uri("/api/chatbot/models")
                .retrieve()
                .body(ModelListResponse.class);
    }

    /**
     * Health check
     */
    public MessageResponse healthCheck() {
        log.debug("Performing health check");
        return restClient.get()
                .uri("/api/chatbot/health")
                .retrieve()
                .body(MessageResponse.class);
    }

    /**
     * Clear cache
     */
    public MessageResponse clearCache() {
        log.debug("Clearing chatbot cache");
        return restClient.post()
                .uri("/api/chatbot/cache/clear")
                .retrieve()
                .body(MessageResponse.class);
    }

    /**
     * Get service status
     */
    public MessageResponse getStatus() {
        log.debug("Getting chatbot service status");
        return restClient.get()
                .uri("/api/chatbot/status")
                .retrieve()
                .body(MessageResponse.class);
    }

    /**
     * Test streaming endpoint
     */
    public Flux<String> testStream() {
        log.debug("Testing stream endpoint");
        return webClient.get()
                .uri("/api/chatbot/test-stream")
                .retrieve()
                .bodyToFlux(String.class);
    }

    /**
     * Test enhanced streaming with events
     */
    public Flux<String> testStreamEnhanced() {
        log.debug("Testing enhanced stream endpoint");
        return webClient.get()
                .uri("/api/chatbot/test-stream-enhanced")
                .retrieve()
                .bodyToFlux(String.class);
    }
}