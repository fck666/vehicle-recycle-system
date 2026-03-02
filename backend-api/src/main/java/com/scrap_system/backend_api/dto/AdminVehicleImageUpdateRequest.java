package com.scrap_system.backend_api.dto;

import lombok.Data;

@Data
public class AdminVehicleImageUpdateRequest {
    private String imageName;
    private Integer sortOrder;
}

