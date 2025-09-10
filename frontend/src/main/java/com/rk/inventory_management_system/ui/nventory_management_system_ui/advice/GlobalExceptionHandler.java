package com.rk.inventory_management_system.ui.nventory_management_system_ui.advice;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.exception.UIException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.class)
    public String handleClientError(HttpClientErrorException ex, Model model, RedirectAttributes redirectAttributes) {
        log.error("Client error: {}", ex.getMessage());

        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            redirectAttributes.addFlashAttribute("errorMessage", "Resource not found");
            return "redirect:/dashboard";
        }

        model.addAttribute("errorMessage", "Bad request: " + ex.getResponseBodyAsString());
        return "error/4xx";
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public String handleServerError(HttpServerErrorException ex, Model model) {
        log.error("Server error: {}", ex.getMessage());
        model.addAttribute("errorMessage", "Server error occurred. Please try again later.");
        return "error/5xx";
    }

    @ExceptionHandler(ResourceAccessException.class)
    public String handleConnectionError(ResourceAccessException ex, Model model) {
        log.error("Connection error: {}", ex.getMessage());
        model.addAttribute("errorMessage", "Unable to connect to backend service. Please try again later.");
        return "error/5xx";
    }

    @ExceptionHandler(UIException.class)
    public String handleUiException(UIException ex, Model model, RedirectAttributes redirectAttributes) {
        log.error("UI exception: {}", ex.getMessage());

        if (ex.getStatusCode() >= 400 && ex.getStatusCode() < 500) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/dashboard";
        }

        model.addAttribute("errorMessage", ex.getMessage());
        return "error/5xx";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error: ", ex);
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
        return "error/5xx";
    }
}
