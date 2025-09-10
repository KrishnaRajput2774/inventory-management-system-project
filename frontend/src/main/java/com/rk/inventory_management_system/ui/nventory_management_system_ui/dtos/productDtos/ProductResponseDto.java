package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.productDtos;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductCategoryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponseDto {

    private Long productId;
    private String productCode;
    private String attribute;
    private Integer quantitySold;
    private String name;
    private Double actualPrice;
    private Double sellingPrice;
    private String description;
    private Double discount;
    private Integer stockQuantity;
    private ProductCategoryDto category;
    private ProductSupplierResponseDto supplier;
    private String brandName;
    private Integer lowStockThreshold;

}
