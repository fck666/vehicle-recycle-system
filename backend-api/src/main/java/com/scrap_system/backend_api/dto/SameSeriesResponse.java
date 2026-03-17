package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SameSeriesResponse {
    private Long targetVehicleId;
    private String targetSeriesName;
    private Integer yearWindow;
    private Integer candidateCount;
    private Integer highConfidenceCount;
    private Integer mediumConfidenceCount;
    private List<SameSeriesCandidateDto> candidates;
}
