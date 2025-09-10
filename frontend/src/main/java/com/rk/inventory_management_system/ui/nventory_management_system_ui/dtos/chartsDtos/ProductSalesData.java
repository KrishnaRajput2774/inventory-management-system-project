package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.chartsDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesData {
    private String productName;
    private Integer quantitySold;
    private Double revenue;
    private Double profit;
}

