package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockResponseDto {

    private String productName;
    private List<ProductStockDetailsDto> productStockDetails;
    private Integer totalStock;
}
