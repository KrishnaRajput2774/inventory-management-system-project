package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelListResponse {
    private List<String> models;
    private boolean success;
}