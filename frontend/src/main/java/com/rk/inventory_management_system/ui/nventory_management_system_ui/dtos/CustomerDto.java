package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    private Long customerId;
    private String name;
    private String contactNumber;
    private String email;
    private String address;
    private List<OrderDto> orders;
    private LocalDateTime createdAt;


    // Additional fields for enhanced analytics (calculated in controller)
    private String customerStatus; // VIP, PREMIUM, ACTIVE, INACTIVE, NEW
    private Double revenue; // for ui only
    private Integer orderCount;
    private Boolean hasRecentOrder;

}
