package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExternalVehicleTrimUpsertRequest {
    private String source;
    private String sourceTrimId;
    private String brand;
    private String seriesName;
    private String marketName;
    private Integer modelYear;
    private String energyType;
    private BigDecimal officialPrice;
    private BigDecimal batteryKwh;
    private Integer displacementMl;
    private BigDecimal powerKw;
    private BigDecimal curbWeight;
    private String coverUrl;
    private String pageUrl;
    private String rawJson;
}

