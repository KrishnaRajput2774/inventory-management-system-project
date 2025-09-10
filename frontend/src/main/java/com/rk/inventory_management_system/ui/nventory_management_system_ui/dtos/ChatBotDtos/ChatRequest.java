package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatRequest {

    private String message;
    private String model;
    private List<Message> history;

}
