package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.ChatbotClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatResponse;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ModelListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotClient chatbotClient;

    @GetMapping
    public String chatPage(Model model) {
        try {
            // Get available models for the dropdown
            ModelListResponse modelResponse = chatbotClient.getAvailableModels();
            if (modelResponse != null && modelResponse.isSuccess()) {
                model.addAttribute("availableModels", modelResponse.getModels());
            } else {
                model.addAttribute("availableModels", List.of("llama3.1")); // Default model
            }

            // Check service health
            try {
                chatbotClient.healthCheck();
                model.addAttribute("serviceStatus", "online");
            } catch (Exception e) {
                model.addAttribute("serviceStatus", "offline");
                log.warn("Chatbot service health check failed", e);
            }

            return "chatbot/chat";

        } catch (Exception e) {
            log.error("Error loading chat page", e);
            model.addAttribute("error", "Unable to load chat interface");
            model.addAttribute("availableModels", List.of("llama3.1"));
            model.addAttribute("serviceStatus", "error");
            return "chatbot/chat";
        }
    }

    /**
     * Send a non-streaming chat message
     */
    @PostMapping("/send")
    @ResponseBody
    public ChatResponse sendMessage(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());
        try {
            return chatbotClient.sendMessage(request);
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ChatResponse.builder()
                    .message("I apologize, but I encountered an error. Please try again.")
                    .error(true)
                    .errorMessage(e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }


    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<String>> streamMessage(@RequestBody ChatRequest request) {
        log.info("Received streaming chat request: {}", request.getMessage());

        return chatbotClient.sendMessageStream(request)
                .map(data -> ServerSentEvent.<String>builder()
                        .data(data)
                        .build())
                .doOnError(error -> log.error("Streaming error", error))
                .onErrorResume(error -> Flux.just(
                        ServerSentEvent.<String>builder()
                                .data("Error: " + error.getMessage())
                                .build()
                ));
    }


    @PostMapping(value = "/stream-json", produces = MediaType.APPLICATION_NDJSON_VALUE)
    @ResponseBody
    public Flux<ChatResponse> streamMessageJson(@RequestBody ChatRequest request) {
        log.info("Received JSON streaming chat request: {}", request.getMessage());

        return chatbotClient.sendMessageStreamJson(request)
                .doOnError(error -> log.error("JSON streaming error", error))
                .onErrorResume(error -> Flux.just(
                        ChatResponse.builder()
                                .message("Error occurred: " + error.getMessage())
                                .error(true)
                                .errorMessage(error.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .build()
                ));
    }

    @GetMapping("/models")
    @ResponseBody
    public ModelListResponse getModels() {
        try {
            return chatbotClient.getAvailableModels();
        } catch (Exception e) {
            log.error("Error fetching models", e);
            return new ModelListResponse(List.of("llama3.1"), false);
        }
    }

    @PostMapping("/cache/clear")
    @ResponseBody
    public String clearCache() {
        try {
            chatbotClient.clearCache();
            return "Cache cleared successfully";
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            return "Error clearing cache: " + e.getMessage();
        }
    }

    @GetMapping("/status")
    @ResponseBody
    public String getStatus() {
        try {
            var status = chatbotClient.getStatus();
            return status != null ? status.getMessage() : "Unknown status";
        } catch (Exception e) {
            log.error("Error getting status", e);
            return "Service unavailable";
        }
    }


}