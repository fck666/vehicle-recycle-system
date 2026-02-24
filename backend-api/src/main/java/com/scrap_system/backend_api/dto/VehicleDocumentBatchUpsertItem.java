package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VehicleDocumentBatchUpsertItem {
    private String productNo;
    private String productId;

    private String docType;
    private String docName;
    private String docUrl;
    private String sha256;
    private String sourceUrl;
    private LocalDateTime fetchedAt;
}
