package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.BatchUpsertResult;
import com.scrap_system.backend_api.dto.DcdSeriesImportRequest;
import com.scrap_system.backend_api.service.DcdImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/external-trims/dcd")
@RequiredArgsConstructor
public class AdminDcdImportController {

    private final DcdImportService dcdImportService;

    @PostMapping("/import")
    public ResponseEntity<BatchUpsertResult> importSeries(@RequestBody DcdSeriesImportRequest request) {
        if (request == null || request.getSeriesIds() == null || request.getSeriesIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(dcdImportService.importSeries(request.getSeriesIds(), request.getCityName()));
    }
}

