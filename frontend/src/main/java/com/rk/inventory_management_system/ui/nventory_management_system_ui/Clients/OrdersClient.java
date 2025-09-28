package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrdersClient {

    private final RestClient restClient;
    private final static String PREFIX_URL = "api/orders";

    public List<OrderDto> findAll() {
        return restClient.get()
                .uri(PREFIX_URL+"/all")
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderDto>>() {});
    }

    public OrderDto create(OrderDto orderDto) {
        log.debug("Creating order");

        return restClient.post()
                .uri(PREFIX_URL+"/create")
                .body(orderDto)
                .retrieve()
                .body(OrderDto.class);
    }

    public OrderDto cancel(Long orderId) {
        log.debug("Cancelling order: {}",orderId);

        return restClient.post()
                .uri(PREFIX_URL+"/{orderID}/cancel",orderId)
                .retrieve()
                .body(OrderDto.class);
    }

    public OrderDto complete(Long orderId) {
        log.debug("Completing order: {}",orderId);

        return restClient.post()
                .uri(PREFIX_URL+"/{orderID}/complete",orderId)
                .retrieve()
                .body(OrderDto.class);
    }

    public OrderDto findOrderById(Long orderId) {
        log.debug("Finding order by id: {}",orderId);

        return restClient.get()
                .uri(PREFIX_URL+"/{orderId}",orderId)
                .retrieve()
                .body(OrderDto.class);
    }

    public void updateStatus(Long orderId, String orderStatus) {
        log.debug("Updating order status to {}, of {}",orderStatus,orderId);

        restClient.post()
                .uri(PREFIX_URL + "/{orderId}/status?orderStatus={orderStatus}",
                        orderId, orderId, orderStatus)
                .retrieve()
                .body(String.class);
    }

    public List<OrderDto> findAllOrdersOfCustomer(Long customerId) {
        log.debug("Finding all orders of customer: {}",customerId);

        return restClient.get()
                .uri(PREFIX_URL+"/customer/{customerId}",customerId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderDto>>() {});
    }

}
