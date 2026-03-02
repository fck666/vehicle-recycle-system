package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VehicleMappingCandidateDto {
    private Long externalTrimId;
    private String source;
    private String sourceTrimId;
    private String brand;
    private String seriesName;
    private String marketName;
    private Integer modelYear;
    private String energyType;
    private String coverUrl;
    private String pageUrl;
    private Double score;
    private Integer rankNo;
}

