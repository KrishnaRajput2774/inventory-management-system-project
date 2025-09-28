package com.rk.inventory_management_system.ui.nventory_management_system_ui.config;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Arrays;

@Slf4j
@Configuration
public class RestClientConfig {


    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8081") // Use UI server (proxy)
                .requestInterceptor((request, body, execution) -> {
                    // Get the current HTTP request from the thread-local context
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest httpRequest = attributes.getRequest();

                        // Extract JWT token from cookies
                        String token = getTokenFromCookies(httpRequest);
                        if (token != null && !token.trim().isEmpty()) {
                            // Forward the entire cookie header to preserve all cookies
                            String cookieHeader =   getCookieHeader(httpRequest);
                            if (cookieHeader != null) {
                                request.getHeaders().set("Cookie", cookieHeader);
                                log.debug("Forwarding cookies to proxy: {}", cookieHeader);
                            }
                        } else {
                            log.warn("No authentication token found in cookies");
                        }
                    } else {
                        log.warn("No request context available for cookie forwarding");
                    }

                    return execution.execute(request, body);
                })
                .build();
    }



    private String getTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "token".equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }

    private String getCookieHeader(HttpServletRequest request) {
        if (request.getCookies() != null) {
            StringBuilder cookieHeader = new StringBuilder();
            for (Cookie cookie : request.getCookies()) {
                if (cookieHeader.length() > 0) {
                    cookieHeader.append("; ");
                }
                cookieHeader.append(cookie.getName()).append("=").append(cookie.getValue());
            }
            return cookieHeader.toString();
        }
        return null;
    }
}