package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.OrderStatus;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.OrderType;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long orderId;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;
    private PaymentType paymentType;

    private CustomerDto customer;
    private SupplierDto supplier;
    private List<OrderItemDto> orderItems;
    private Double totalPrice;
}
