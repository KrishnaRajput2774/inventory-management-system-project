package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryDto {
    private Long id;
    private String name;
    private String status = "ACTIVE";
    private Integer productCount;
    private LocalDate createdDate;
    private String description;
}
