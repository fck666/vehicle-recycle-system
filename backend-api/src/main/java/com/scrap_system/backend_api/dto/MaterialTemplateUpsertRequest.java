package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialTemplateUpsertRequest {
    private String vehicleType;
    private BigDecimal steelRatio;
    private BigDecimal aluminumRatio;
    private BigDecimal copperRatio;
    private BigDecimal recoveryRatio;
}

