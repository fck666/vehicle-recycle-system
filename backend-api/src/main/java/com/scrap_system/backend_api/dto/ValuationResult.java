package com.scrap_system.backend_api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ValuationResult {
    private BigDecimal totalValue;
    private ValuationDimension exactMatch;
    private ValuationDimension seriesHighMatch;
    private ValuationDimension seriesMediumMatch;
}
