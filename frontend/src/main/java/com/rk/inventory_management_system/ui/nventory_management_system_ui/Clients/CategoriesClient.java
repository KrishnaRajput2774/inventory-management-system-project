package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductCategoryDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CategoriesClient {

    private final RestClient restClient;

    public ProductCategoryDto create(ProductCategoryDto categoryDto) {
        log.debug("Creating Product Category: {}", categoryDto.getName());
        return restClient.post()
                .uri("/api/category/create")
                .body(categoryDto)
                .retrieve()
                .body(ProductCategoryDto.class);
    }

    public ProductCategoryDto delete(Long categoryId) {
        log.debug("Deleting Product Category: {}", categoryId);

        return restClient.delete()
                .uri("api/category/{categoryId}/delete",categoryId)
                .retrieve()
                .body(ProductCategoryDto.class);
    }

    public List<ProductCategoryDto> findAll() {
        log.debug("Finding All Product Categories");

        return restClient.get()
                .uri("/api/category/all")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductCategoryDto>>() {});
    }

    public ProductCategoryDto findById(Long id) {
        return restClient.get()
                .uri("/api/category/{id}",id)
                .retrieve()
                .body(ProductCategoryDto.class);
    }

    public List<ProductDto> findProductsByCategoryId(Long categoryId) {
        log.debug("Finding all products of category: {}",categoryId);

        return restClient.get()
                .uri("api/category/{categoryId}/products",categoryId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductDto>>() {});
    }

}
