package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MaterialPriceUpsertRequest {
    private String type;
    private BigDecimal pricePerKg;
    private String currency;
    private String unit;
    private LocalDate effectiveDate;
    private LocalDateTime fetchedAt;
    private String sourceName;
    private String sourceUrl;
    private String rawPayload;
}
