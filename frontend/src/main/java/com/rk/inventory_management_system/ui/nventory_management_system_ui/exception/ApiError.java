package com.rk.inventory_management_system.ui.nventory_management_system_ui.exception;

public class ApiError extends RuntimeException{

    public ApiError() {
        super();
    }

    public ApiError(String message) {
        super(message);
    }

    public ApiError(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiError(Throwable cause) {
        super(cause);
    }

    protected ApiError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
