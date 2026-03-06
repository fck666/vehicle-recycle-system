package com.scrap_system.backend_api.dto;

import com.scrap_system.backend_api.model.enums.VehicleSourceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VehicleUpsertRequest {
    private VehicleSourceType sourceType;
    private String brand;
    private String model;
    private Integer modelYear;
    private String fuelType;
    private BigDecimal curbWeight;
    private BigDecimal batteryKwh;
    private String vehicleType;

    private String productNo;
    private String productId;
    private String productModel;
    private String manufacturerName;
    private String trademark;
    private String productionAddress;
    private String registrationAddress;
    private LocalDate releaseDate;
    private LocalDate effectiveDate;
    private Integer batchNo;
    private Integer catalogIndex;

    private Integer lengthMm;
    private Integer widthMm;
    private Integer heightMm;
    private Integer wheelbaseMm;
    private Integer frontOverhangMm;
    private Integer rearOverhangMm;
    private BigDecimal approachAngleDeg;
    private BigDecimal departureAngleDeg;
    private Integer frontTrackMm;
    private Integer rearTrackMm;
    private Integer axleCount;
    private String axleLoadKg;
    private Integer tireCount;
    private String tireSpec;
    private String steeringType;
    private Boolean hasAbs;
    private Integer maxSpeedKmh;
    private BigDecimal grossWeight;
    private String motorModel;
    private String motorManufacturer;
    private Integer displacementMl;
    private BigDecimal powerKw;
    private String vinPattern;
    private String chassisModel;
    private String sourceSite;
    private String specRawJson;
}
