package com.scrap_system.backend_api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ValuationResult {
    private BigDecimal totalValue;
    private BigDecimal steelValue;
    private BigDecimal aluminumValue;
    private BigDecimal copperValue;
    private BigDecimal batteryValue;
    private List<MaterialValueItem> materialValues;
}
