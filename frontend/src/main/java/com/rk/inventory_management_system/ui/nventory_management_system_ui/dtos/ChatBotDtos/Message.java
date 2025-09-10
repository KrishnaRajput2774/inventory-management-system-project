package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Message {

    private String content;
    private String role;

}
