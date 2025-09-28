package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long productId;
    private String productCode;
    private String attribute;
    private String name;
    private String description;
    private Double actualPrice;
    private Double sellingPrice;
    private Double discount;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private ProductCategoryDto category;
    private SupplierDto supplier;
    private String brandName;
    private Integer quantitySold;
}