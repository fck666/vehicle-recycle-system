package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValuationDimension {
    private int recordCount;
    private BigDecimal avgValue;
    private BigDecimal minValue;
    private BigDecimal maxValue;
}