package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;


import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.CustomerDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.OrderDto;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomersClient {

    private final RestClient restClient;

    public CustomerDto create(CustomerDto customerDto) {
        log.debug("Creating Customer: {}",customerDto.getName());
        return restClient.post()
                .uri("/api/customer/create")
                .body(customerDto)
                .retrieve()
                .body(CustomerDto.class);

    }

    public CustomerDto findCustomerById(Long customerId) {
        log.debug("Finding customer by id: {}",customerId);
        return restClient.get()
                .uri("/api/customer/{customerID}",customerId)
                .retrieve()
                .body(CustomerDto.class);
    }

    public List<CustomerDto> findAll(){
        log.debug("Finding all customers");
        return restClient.get()
                .uri("/api/customer/all")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CustomerDto>>(){});
    }

    public List<OrderDto> findOrdersByCustomerId(Long customerID) {
        log.debug("Finding orders for customer: {}",customerID);
        return restClient.get()
                .uri("/api/customer/{customerID}/order",customerID)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderDto>>() {});
    }
}
