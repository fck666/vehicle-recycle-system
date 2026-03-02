package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class VehicleMappingRowDto {
    private Long miitVehicleId;
    private String brand;
    private String model;
    private Integer modelYear;
    private String fuelType;
    private BigDecimal batteryKwh;
    private BigDecimal curbWeight;
    private String productId;
    private String productNo;
    private String status;
    private Long externalTrimId;
    private String externalMarketName;
    private String externalPageUrl;
    private String externalCoverUrl;
    private List<VehicleMappingCandidateDto> candidates;
}

