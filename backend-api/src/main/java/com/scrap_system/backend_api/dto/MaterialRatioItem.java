package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialRatioItem {
    private String materialType;
    private BigDecimal ratio;
    private String pricingMode;
    private BigDecimal fixedTotalPrice;
}
