package com.rk.inventory_management_system.ui.nventory_management_system_ui.exception;

import lombok.Getter;

@Getter
public class UIException extends RuntimeException{
    private final int statusCode;

    public UIException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public UIException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

}
