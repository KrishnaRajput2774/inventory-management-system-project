package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
    public class OrderItemDto {

        private Long orderItemId;
        private ProductDto productDto;
        private OrderDto orderDto;
        private Double priceAtOrderTime;
        private Integer quantity;

    }
