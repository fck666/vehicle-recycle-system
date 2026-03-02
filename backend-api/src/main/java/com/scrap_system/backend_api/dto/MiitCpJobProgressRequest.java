package com.scrap_system.backend_api.dto;

import lombok.Data;

@Data
public class MiitCpJobProgressRequest {
    private Integer inserted;
    private Integer updated;
    private Integer skipped;
    private String message;
    private String detailsJson;
}

