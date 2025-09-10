package com.rk.inventory_management_system.ui.nventory_management_system_ui.advice;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiError {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;


}
