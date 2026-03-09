package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MaterialTemplateUpsertRequest {
    private String vehicleType;
    private String scopeType;
    private String scopeValue;
    private BigDecimal recoveryRatio;
    private BigDecimal othersPricePerKgOverride;
    private List<MaterialRatioItem> materials;
}
