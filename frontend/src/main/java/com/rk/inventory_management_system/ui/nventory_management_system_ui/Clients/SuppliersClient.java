package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.SupplierDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.supplierDtos.SupplierProductsResponseDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.supplierDtos.SupplierResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuppliersClient {

    private final RestClient restClient;

    public SupplierDto create(SupplierDto supplierDto) {
        log.debug("Creating supplier: {}",supplierDto.getName());

        return restClient.post()
                .uri("/api/supplier/create")
                .body(supplierDto)
                .retrieve()
                .body(SupplierDto.class);
    }

    public SupplierDto delete(Long supplierId) {
        log.debug("Deleting supplier: {}",supplierId);

        return restClient.delete()
                .uri("/api/supplier/{supplierId}",supplierId)
                .retrieve()
                .body(SupplierDto.class);
    }

    public List<SupplierResponseDto> findAll() {
        log.debug("Finding all suppliers");

        return restClient.get()
                .uri("/api/supplier/all")
                .retrieve()
                .body(new ParameterizedTypeReference<List<SupplierResponseDto>>() {});
    }

    public SupplierResponseDto findSupplierById(Long supplierId) {

        return restClient.get()
                .uri("/api/supplier/{supplierId}",supplierId)
                .retrieve()
                .body(SupplierResponseDto.class);
    }

    public List<SupplierProductsResponseDto> findAllProductsBySupplierId(Long supplierId) {
        log.debug("Finding all products of supplier: {}",supplierId);

        return restClient.get()
                .uri("/api/supplier/{supplierId}/products",supplierId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<SupplierProductsResponseDto>>() {});
    }

}
