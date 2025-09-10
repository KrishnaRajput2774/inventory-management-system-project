package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierDto {

    private Long id;
    private String name;
    private String contactNumber;
    private String email;
    private String address;
    private List<ProductDto> products;
    private Integer productsCount;
    private LocalDateTime createdAt;

}
