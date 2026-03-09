package com.scrap_system.backend_api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaterialSourceSuggestResult {
    private String type;
    private String displayName;
    private String sourceName;
    private String sourceUrl;
    private String parseKeyword;
}
