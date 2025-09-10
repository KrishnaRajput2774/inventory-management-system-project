package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;

@Slf4j
@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirect", required = false) String redirect,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "error", required = false) String error,
                            Model model,
                            HttpServletRequest request) {

        // Check if user is already logged in
        if (isUserLoggedIn(request)) {
            log.debug("User already logged in, redirecting to dashboard");
            return "redirect:/dashboard";
        }

        if ("true".equals(logout)) {
            model.addAttribute("successMessage", "You have been successfully logged out.");
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Login failed. Please check your credentials.");
        }

        if (redirect != null) {
            model.addAttribute("redirectUrl", redirect);
            log.debug("Redirect URL after login: {}", redirect);
        }

        return "auth/login";
    }

//
//    @GetMapping("/dashboard")
//    public String dashboard(HttpServletRequest request) {
//        // This will be intercepted by JwtTokenInterceptor if no valid token
//        log.debug("Accessing dashboard");
//        return "dashboard"; // Return your dashboard template
//    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        // Clear the token cookie
        Cookie tokenCookie = new Cookie("token", "");
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(0); // Expire immediately
        tokenCookie.setHttpOnly(true);
        response.addCookie(tokenCookie);

        log.debug("User logged out, token cookie cleared");
        return "redirect:/login?logout=true";
    }

    private boolean isUserLoggedIn(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .anyMatch(cookie -> "token".equals(cookie.getName())
                            && cookie.getValue() != null
                            && !cookie.getValue().trim().isEmpty());
        }
        return false;
    }
}