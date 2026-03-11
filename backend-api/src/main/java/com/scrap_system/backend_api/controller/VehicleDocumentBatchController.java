package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.BatchUpsertResult;
import com.scrap_system.backend_api.dto.VehicleDocumentBatchUpsertItem;
import com.scrap_system.backend_api.dto.VehicleDocumentBatchUpsertRequest;
import com.scrap_system.backend_api.model.JobRun;
import com.scrap_system.backend_api.model.VehicleDocument;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.VehicleDocumentRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.service.JobRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/vehicle-documents")
@RequiredArgsConstructor
public class VehicleDocumentBatchController {

    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final JobRunService jobRunService;

    @PostMapping("/batch")
    public ResponseEntity<BatchUpsertResult> batchUpsert(
            @RequestBody VehicleDocumentBatchUpsertRequest request,
            @RequestHeader(value = "X-Run-Id", required = false) String runId,
            Authentication authentication
    ) {
        Long userId = authentication != null && authentication.getPrincipal() instanceof Long ? (Long) authentication.getPrincipal() : null;
        JobRun jr = jobRunService.start("MIIT_VEHICLE_DOCS_UPSERT", runId, userId, userId == null ? null : ("user:" + userId),
                request == null || request.getItems() == null ? null : ("{\"count\":" + request.getItems().size() + "}"));
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            jobRunService.success(jr, 0, 0, 0, "empty", null);
            return ResponseEntity.ok(new BatchUpsertResult(0, 0, 0));
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        try {
            for (VehicleDocumentBatchUpsertItem item : request.getItems()) {
                if (item == null || isBlank(item.getDocUrl())) {
                    skipped++;
                    continue;
                }

                String productId = normalize(item.getProductId());
                String productNo = normalize(item.getProductNo());
                if (isBlank(productId) && isBlank(productNo)) {
                    skipped++;
                    continue;
                }

                Optional<VehicleModel> vehicleOpt = resolveVehicle(productId, productNo);
                if (vehicleOpt.isEmpty()) {
                    skipped++;
                    continue;
                }

                VehicleModel vehicle = vehicleOpt.get();
                VehicleDocument doc = resolveExisting(vehicle.getId(), item).orElseGet(VehicleDocument::new);
                boolean isInsert = doc.getId() == null;

                doc.setVehicle(vehicle);
                doc.setDocType(item.getDocType());
                doc.setDocName(item.getDocName());
                doc.setDocUrl(item.getDocUrl());
                doc.setSha256(item.getSha256());
                doc.setSourceUrl(item.getSourceUrl());
                doc.setFetchedAt(item.getFetchedAt());

                VehicleDocument saved = vehicleDocumentRepository.save(doc);
                if (isInsert) {
                    vehicle.getDocuments().add(saved);
                    vehicleModelRepository.save(vehicle);
                    inserted++;
                } else {
                    updated++;
                }
            }
            jobRunService.success(jr, inserted, updated, skipped, null, null);
        } catch (Exception e) {
            jobRunService.failed(jr, e.getMessage(), null);
            throw e;
        }

        return ResponseEntity.ok(new BatchUpsertResult(inserted, updated, skipped));
    }

    private Optional<VehicleDocument> resolveExisting(Long vehicleId, VehicleDocumentBatchUpsertItem item) {
        if (vehicleId == null || item == null) return Optional.empty();
        if (!isBlank(item.getSha256())) {
            return vehicleDocumentRepository.findFirstByVehicle_IdAndSha256(vehicleId, item.getSha256().trim());
        }
        if (!isBlank(item.getDocUrl())) {
            return vehicleDocumentRepository.findFirstByVehicle_IdAndDocUrl(vehicleId, item.getDocUrl().trim());
        }
        return Optional.empty();
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim();
    }

    private Optional<VehicleModel> resolveVehicle(String productId, String productNo) {
        if (!isBlank(productNo)) {
            Optional<VehicleModel> byNo = vehicleModelRepository.findByProductNo(productNo);
            if (byNo.isPresent()) {
                return byNo;
            }
        }
        
        // Strict Mode: Do NOT fallback to productId if productNo is present but not found.
        if (isBlank(productNo) && !isBlank(productId)) {
            return vehicleModelRepository.findByProductId(productId);
        }
        
        return Optional.empty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
