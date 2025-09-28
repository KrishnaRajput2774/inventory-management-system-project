package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ChatBotDtos.ChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiProxyController {

    @Value("${backend.base-url}")
    private String backendUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    // ---------------- AUTH ----------------
    @PostMapping("/auth/login")
    @ResponseBody
    public ResponseEntity<String> proxyLogin(@RequestBody String requestBody,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        try {
            log.debug("Proxying login request to backend: {}", backendUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> backendResponse = restTemplate.exchange(
                    backendUrl + "/api/auth/login",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Forward cookies
            forwardCookies(backendResponse, response);

            return ResponseEntity.status(backendResponse.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)   // ✅ Ensure JSON type
                    .body(backendResponse.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Backend login failed: {}", e.getStatusCode(), e);
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Login proxy error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Login failed: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/auth/logout")
    @ResponseBody
    public ResponseEntity<String> proxyLogout(HttpServletRequest request,
                                              HttpServletResponse response) {
        return proxyRequest(request, response, null, "POST", "/auth/logout");
    }

    // ---------------- CUSTOMER ----------------
    @GetMapping("/customer/all")
    @ResponseBody
    public ResponseEntity<String> getAllCustomers(HttpServletRequest request,
                                                  HttpServletResponse response) {
        return proxyRequest(request, response, null, "GET", "/customer/all");
    }

    @PostMapping("/customer/create")
    @ResponseBody
    public ResponseEntity<String> createCustomer(@RequestBody String requestBody,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        return proxyRequest(request, response, requestBody, "POST", "/customer/create");
    }

    // ---------------- PRODUCT ----------------
    @GetMapping("/product/all")
    @ResponseBody
    public ResponseEntity<String> getAllProducts(HttpServletRequest request,
                                                 HttpServletResponse response) {
        return proxyRequest(request, response, null, "GET", "/product/all");
    }

    @PostMapping("/product/create")
    @ResponseBody
    public ResponseEntity<String> createProduct(@RequestBody String requestBody,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        return proxyRequest(request, response, requestBody, "POST", "/product/create");
    }

    // ---------------- SUPPLIER ----------------
    @GetMapping("/supplier/all")
    @ResponseBody
    public ResponseEntity<String> getAllSuppliers(HttpServletRequest request,
                                                  HttpServletResponse response) {
        return proxyRequest(request, response, null, "GET", "/supplier/all");
    }

    @PostMapping("/supplier/create")
    @ResponseBody
    public ResponseEntity<String> createSupplier(@RequestBody String requestBody,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        return proxyRequest(request, response, requestBody, "POST", "/supplier/create");
    }

    // ---------------- CATEGORY ----------------
    @GetMapping("/category/all")
    @ResponseBody
    public ResponseEntity<String> getAllCategories(HttpServletRequest request,
                                                   HttpServletResponse response) {
        return proxyRequest(request, response, null, "GET", "/category/all");
    }

    @PostMapping("/category/create")
    @ResponseBody
    public ResponseEntity<String> createCategory(@RequestBody String requestBody,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        return proxyRequest(request, response, requestBody, "POST", "/category/create");
    }

    // ---------------- ORDER ----------------
    @GetMapping("/order/all")
    @ResponseBody
    public ResponseEntity<String> getAllOrders(HttpServletRequest request,
                                               HttpServletResponse response) {
        return proxyRequest(request, response, null, "GET", "/order/all");
    }

    @PostMapping("/order/create")
    @ResponseBody
    public ResponseEntity<String> createOrder(@RequestBody String requestBody,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        return proxyRequest(request, response, requestBody, "POST", "/order/create");
    }

    // ---------------- GENERIC PROXY ----------------
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    @ResponseBody
    public ResponseEntity<String> proxyGenericRequest(HttpServletRequest request,
                                                      HttpServletResponse response,
                                                      @RequestBody(required = false) String requestBody) {
        String path = request.getRequestURI().substring("/api".length());
        String method = request.getMethod();
        return proxyRequest(request, response, requestBody, method, path);
    }

    // ---------------- SHARED PROXY LOGIC ----------------
    private ResponseEntity<String> proxyRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                String requestBody,
                                                String method,
                                                String path) {
        try {
            log.debug("Proxying {} request to: {}{}", method, backendUrl, path);

            HttpHeaders headers = new HttpHeaders();

            // Forward content type
            String contentType = request.getContentType();
            if (contentType != null) {
                headers.setContentType(MediaType.parseMediaType(contentType));
            }

            // Forward cookies
            String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null) {
                headers.set("Cookie", cookieHeader);
                log.debug("Forwarding cookies: {}", cookieHeader);
            }

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> backendResponse = restTemplate.exchange(
                    backendUrl + "/api" + path,
                    HttpMethod.valueOf(method),
                    entity,
                    String.class
            );

            // Forward cookies
            forwardCookies(backendResponse, response);

            return ResponseEntity.status(backendResponse.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)  // ✅ Always return JSON
                    .body(backendResponse.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Backend {} request to {} failed: {}", method, path, e.getStatusCode(), e);
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Proxy {} request to {} error", method, path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Request failed: " + e.getMessage() + "\"}");
        }
    }

    private void forwardCookies(ResponseEntity<String> backendResponse, HttpServletResponse response) {
        List<String> setCookieHeaders = backendResponse.getHeaders().get("Set-Cookie");
        if (setCookieHeaders != null) {
            for (String cookie : setCookieHeaders) {
                response.addHeader("Set-Cookie", cookie);
            }
        }
    }

    @GetMapping("/invoices/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadInvoice(
            @RequestParam("orderIds") List<Long> orderIds,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            log.debug("Proxying invoice download for orders: {}", orderIds);

            HttpHeaders headers = new HttpHeaders();

            // ✅ Forward Authorization header
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            }

            // ✅ Forward cookies if present
            String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null) {
                headers.set("Cookie", cookieHeader);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> backendResponse = restTemplate.exchange(
                    backendUrl + "/api/invoices/download?orderIds=" + String.join(",",
                            orderIds.stream().map(String::valueOf).toList()),
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            // ✅ Forward cookies back to client
            forwardCookies((ResponseEntity) backendResponse, response);

            return ResponseEntity.status(backendResponse.getStatusCode())
                    .headers(backendResponse.getHeaders())
                    .body(backendResponse.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Invoice download failed: {}", e.getStatusCode(), e);
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            log.error("Invoice proxy error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"Invoice download failed: " + e.getMessage() + "\"}").getBytes());
        }
    }
}
