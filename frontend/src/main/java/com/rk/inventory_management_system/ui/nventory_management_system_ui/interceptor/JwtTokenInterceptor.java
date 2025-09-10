package com.rk.inventory_management_system.ui.nventory_management_system_ui.interceptor;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


import java.util.Arrays;

@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();

        // Skip authentication for these paths
        if (requestURI.equals("/login") ||
                requestURI.startsWith("/api/auth/") ||
                requestURI.startsWith("/css/") ||
                requestURI.startsWith("/js/") ||
                requestURI.startsWith("/images/") ||
                requestURI.startsWith("/static/") ||
                requestURI.startsWith("/webjars/") ||
                requestURI.equals("/favicon.ico") ||
                requestURI.equals("/error")) {
            return true;
        }

        // Check for token in cookies
        String token = null;
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(cookie -> "token".equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }

        if (token == null || token.trim().isEmpty()) {
            // No token found, redirect to login
            String redirectUrl = request.getRequestURL().toString();
            if (request.getQueryString() != null) {
                redirectUrl += "?" + request.getQueryString();
            }

            response.sendRedirect("/login?redirect=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8"));
            return false;
        }

        // Add token to request header for backend API calls
        request.setAttribute("authToken", token);

        return true;
    }


}