package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class SameSeriesCandidateDto {
    private Long vehicleId;
    private String brand;
    private String model;
    private Integer modelYear;
    private String manufacturerName;
    private String vehicleType;
    private String fuelType;
    private BigDecimal curbWeight;
    private Integer wheelbaseMm;
    private String seriesName;
    private Integer score;
    private String confidenceLevel;
    private List<String> matchReasons;
}
