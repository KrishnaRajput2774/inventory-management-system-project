package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.OrderDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.OrderItemDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.RemoveOrderItemRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderItemClient {

    private final RestClient restClient;
    private final static String PREFIX_URL = "api/orderItem";

    public OrderDto addItemToOrder(Long orderId, OrderItemDto orderItemDto) {
        log.debug("Adding item to order: {}", orderId);

        return restClient.post()
                .uri(PREFIX_URL + "/{orderId}", orderId)
                .body(orderItemDto)
                .retrieve()
                .body(OrderDto.class);
    }

    public List<OrderItemDto> removeItemFromOrder(Long orderId, RemoveOrderItemRequestDto itemRequestDto) {
        log.debug("Removing item from order: {}, itemId: {}, quantity: {}",
                orderId, itemRequestDto.getOrderItemId(), itemRequestDto.getQuantityToRemove());

        return restClient.method(HttpMethod.DELETE)
                .uri(PREFIX_URL + "/{orderId}", orderId)
                .body(itemRequestDto)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderItemDto>>() {});
    }

    public OrderDto getAllItemsByOrder(Long orderId) {
        log.debug("Getting all items for order: {}", orderId);

        return restClient.get()
                .uri(PREFIX_URL + "/{orderId}", orderId)
                .retrieve()
                .body(OrderDto.class);
    }
}

