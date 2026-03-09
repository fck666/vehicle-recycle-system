package com.scrap_system.backend_api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MaterialTemplateDto {
    private Long id;
    private String vehicleType;
    private BigDecimal recoveryRatio;
    private LocalDateTime createdAt;
    private List<MaterialRatioItem> materials;
}
