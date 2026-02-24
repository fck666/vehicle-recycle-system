package com.scrap_system.backend_api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PreciseValuationRequest {
    private BigDecimal curbWeight;
    private BigDecimal steelRatio;
    private BigDecimal aluminumRatio;
    private BigDecimal copperRatio;
    private BigDecimal batteryKwh; // 可选，针对新能源
}
