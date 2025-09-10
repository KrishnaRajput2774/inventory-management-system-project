package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.ChatBotClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatResponse;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatBotClient chatClient;
    private static final String CHAT_HISTORY_SESSION_KEY = "chatHistory";
    private static final String SELECTED_MODEL_SESSION_KEY = "selectedModel";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping
    public String showChatPage(Model model, HttpSession session) {
        List<String> availableModels = null;
        try {
            // Get available models
            availableModels = chatClient.getAllModels();
            String selectedModel = availableModels.contains("llama3") ? "llama3" : availableModels.get(0);

            model.addAttribute("availableModels", availableModels);
            model.addAttribute("selectedModel", selectedModel);
            availableModels.stream().forEach(model1 -> log.info("Model: {} is present", model1));

            // Get or initialize chat history from session
            List<ChatMessage> chatHistory = getChatHistory(session);
            model.addAttribute("chatHistory", chatHistory);

            // Get selected model from session or use default
            String selectedModel1 = getSelectedModel(session, availableModels);
            model.addAttribute("selectedModel1", selectedModel1);

            // Add empty message object for form binding
            model.addAttribute("userMessage", "");

            log.info("Chat page loaded with {} messages in history", chatHistory.size());

        } catch (Exception e) {
            log.error("Error loading chat page", e);
            model.addAttribute("errorMessage", "Unable to load AI models. Please try again later.");
            model.addAttribute("availableModels", availableModels);
            model.addAttribute("chatHistory", new ArrayList<>());
            model.addAttribute("selectedModel", "llama3");
        }

        return "chatbot/chat";
    }

    @PostMapping("/send")
    public String sendMessage(
            @RequestParam("message") String message,
            @RequestParam("model") String model,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model modelAttr) {

        if (message == null || message.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter a message");
            return "redirect:/chat";
        }

        try {
            // Get chat history from session
            List<ChatMessage> chatHistory = getChatHistory(session);

            // Create and add user message
            ChatMessage userMsg = new ChatMessage();
            userMsg.setContent(message);
            userMsg.setRole("user");
            userMsg.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));
            userMsg.setModel(model);
            chatHistory.add(userMsg);

            // Prepare request with conversation history for context
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMessage(message);
            chatRequest.setModel(model);
            chatRequest.setHistory(convertToMessageHistory(chatHistory));

            // Call AI service
            log.info("Sending message to {} model: {}", model, message);
            ChatResponse response = chatClient.chat(chatRequest);

            // Create and add AI response
            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setContent(response.getMessage());
            aiMsg.setRole("assistant");
            aiMsg.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));
            aiMsg.setModel(model);
            chatHistory.add(aiMsg);

            // Update session
            session.setAttribute(CHAT_HISTORY_SESSION_KEY, chatHistory);
            session.setAttribute(SELECTED_MODEL_SESSION_KEY, model);

            log.info("AI response received from {} model", model);
            redirectAttributes.addFlashAttribute("successMessage", "Message sent successfully");

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to get AI response. Please try again.");
        }

        return "redirect:/chat#chat-bottom";
    }

    @PostMapping("/clear")
    public String clearChat(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute(CHAT_HISTORY_SESSION_KEY);
        log.info("Chat history cleared");
        redirectAttributes.addFlashAttribute("infoMessage", "Chat history cleared");
        return "redirect:/chat";
    }


    @PostMapping("/switch-model")
    @ResponseBody
    public Map<String, String> switchModel(
            @RequestParam("model") String model,
            HttpSession session) {

        session.setAttribute(SELECTED_MODEL_SESSION_KEY, model);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("model", model);
        log.info("Switched to {} model", model);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> getChatHistory(HttpSession session) {
        List<ChatMessage> history = (List<ChatMessage>) session.getAttribute(CHAT_HISTORY_SESSION_KEY);
        if (history == null) {
            history = new ArrayList<>();
            session.setAttribute(CHAT_HISTORY_SESSION_KEY, history);
        }
        return history;
    }


    private String getSelectedModel(HttpSession session, List<String> availableModels) {
        String model = (String) session.getAttribute(SELECTED_MODEL_SESSION_KEY);
        if (model == null || !availableModels.contains(model)) {
            model = availableModels.isEmpty() ? "llama3" : availableModels.get(0);
            session.setAttribute(SELECTED_MODEL_SESSION_KEY, model);
        }
        return model;
    }

    private List<Message> convertToMessageHistory(List<ChatMessage> chatHistory) {
        List<Message> messages = new ArrayList<>();
        // Include last 10 messages for context (5 exchanges)
        int startIndex = Math.max(0, chatHistory.size() - 10);

        for (int i = startIndex; i < chatHistory.size(); i++) {
            ChatMessage chatMsg = chatHistory.get(i);
            Message msg = new Message();
            msg.setContent(chatMsg.getContent());
            msg.setRole(chatMsg.getRole());
            messages.add(msg);
        }
        return messages;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatMessage implements java.io.Serializable {
        private String content;
        private String role; // "user" or "assistant"
        private String timestamp;
        private String model;
    }
}