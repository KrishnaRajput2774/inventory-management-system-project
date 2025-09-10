package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;


import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductStockResponseDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.productDtos.ProductResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductsClient {

    private final RestClient restClient;
    private final static String PREFIX_URL = "/api/products";

    public ProductResponseDto create(ProductDto productDto) {
        log.debug("Creating product: {}",productDto.getName());

        return restClient.post()
                .uri(PREFIX_URL+"/create")
                .body(productDto)
                .retrieve()
                .body(ProductResponseDto.class);
    }


    public List<ProductDto> findAll() {
        log.debug("Finding all products: ");

        return restClient.get()
                .uri(PREFIX_URL+"/all")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductDto>>() {});
    }

    public ProductDto findById(Long productId) {
        log.debug("Finding product of id: {}",productId);

        return restClient.get()
                .uri(PREFIX_URL+"/{productId}",productId)
                .retrieve()
                .body(ProductDto.class);
    }

    public ProductStockResponseDto findProductStockById(Long productId) {
        log.debug("Finding product stock of id: {}",productId);

        return restClient.get()
                .uri(PREFIX_URL+"/{productId}/stock",productId)
                .retrieve()
                .body(ProductStockResponseDto.class);
    }

    public List<ProductDto> reduceStock(Long productId, Integer quantityToReduce) {
        log.debug("Reducing product stock by {} of id: {}", quantityToReduce, productId);

        return restClient.post()
                .uri(PREFIX_URL + "/{productId}/stock/reduce/{quantityToReduce}", productId, quantityToReduce)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductDto>>() {});
    }

    public void increaseStock(Long productId, Integer quantityToAdd) {
        log.debug("Increasing product stock by {} of id: {}", quantityToAdd, productId);

        restClient.post()
                .uri(PREFIX_URL + "/{productId}/stock/increase/{quantityToAdd}", productId, quantityToAdd)
                .retrieve()
                .toBodilessEntity();
    }


}
