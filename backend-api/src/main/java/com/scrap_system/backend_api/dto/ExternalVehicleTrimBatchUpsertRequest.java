package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExternalVehicleTrimBatchUpsertRequest {
    private List<ExternalVehicleTrimUpsertRequest> items;
}

