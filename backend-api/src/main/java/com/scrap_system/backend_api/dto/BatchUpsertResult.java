package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatchUpsertResult {
    private int inserted;
    private int updated;
    private int skipped;
}

