package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.VehicleDocumentUpsertRequest;
import com.scrap_system.backend_api.model.VehicleDocument;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.VehicleDocumentRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleDocumentController {

    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;

    @PostMapping("/{vehicleId}/documents")
    public ResponseEntity<VehicleDocument> addDocument(@PathVariable Long vehicleId, @RequestBody VehicleDocumentUpsertRequest request) {
        if (request == null || request.getDocUrl() == null || request.getDocUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        VehicleDocument doc = new VehicleDocument();
        doc.setVehicle(vehicle);
        doc.setDocType(request.getDocType());
        doc.setDocName(request.getDocName());
        doc.setDocUrl(request.getDocUrl());
        doc.setSha256(request.getSha256());
        doc.setSourceUrl(request.getSourceUrl());
        doc.setFetchedAt(request.getFetchedAt());

        VehicleDocument saved = vehicleDocumentRepository.save(doc);
        vehicle.getDocuments().add(saved);
        vehicleModelRepository.save(vehicle);

        return ResponseEntity.ok(saved);
    }
}

