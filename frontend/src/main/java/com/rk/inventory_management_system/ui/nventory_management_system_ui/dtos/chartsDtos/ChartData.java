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
public class ChartData {
    private List<String> labels;
    private List<Number> data;
    private List<String> colors;
}