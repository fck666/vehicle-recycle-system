package com.scrap_system.backend_api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MaterialValueItem {
    private String category;
    private String materialType;
    private BigDecimal ratio;
    private BigDecimal weightKg;
    private BigDecimal pricePerKg;
    private BigDecimal value;
}
