package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockDetailsDto {
    private Long productId;
    private Integer stockQuantity;
    private Integer quantitySold;
    private Integer lowStockThreshold;

    private Long supplierId;
    private String supplierName;
}