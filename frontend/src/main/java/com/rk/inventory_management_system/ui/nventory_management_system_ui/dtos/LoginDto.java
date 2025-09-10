package com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDto {

    private String userName;
    private String password;

}
