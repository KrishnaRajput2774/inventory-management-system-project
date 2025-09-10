package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveOrderItemRequestDto {

    private Long orderItemId;
    private Integer quantityToRemove;

}

