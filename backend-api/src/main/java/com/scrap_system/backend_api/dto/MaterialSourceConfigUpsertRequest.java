package com.scrap_system.backend_api.dto;

import lombok.Data;

@Data
public class MaterialSourceConfigUpsertRequest {
    private String type;
    private String displayName;
    private String sourceName;
    private String sourceUrl;
    private String parseKeyword;
    private Boolean enabled;
}
