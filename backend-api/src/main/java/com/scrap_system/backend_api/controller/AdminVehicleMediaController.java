package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.AdminVehicleImageUpdateRequest;
import com.scrap_system.backend_api.dto.VehicleDocumentUpsertRequest;
import com.scrap_system.backend_api.model.VehicleDocument;
import com.scrap_system.backend_api.model.VehicleImage;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.VehicleDocumentRepository;
import com.scrap_system.backend_api.repository.VehicleImageRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/vehicles")
@RequiredArgsConstructor
public class AdminVehicleMediaController {

    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleImageRepository vehicleImageRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final FileStorageService fileStorageService;

    @PostMapping("/{vehicleId}/images")
    @Transactional
    public ResponseEntity<?> uploadImage(
            @PathVariable Long vehicleId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "sort", required = false, defaultValue = "0") Integer sort
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }

        // Deduplicate: check if image with same name already exists
        if (name != null) {
            boolean exists = vehicle.getImages().stream()
                    .anyMatch(i -> name.equals(i.getImageName()));
            if (exists) {
                // If exists, just return the existing one or OK status
                // But we need to return the object. Let's find it.
                VehicleImage existing = vehicle.getImages().stream()
                        .filter(i -> name.equals(i.getImageName()))
                        .findFirst().orElse(null);
                return ResponseEntity.ok(existing);
            }
        }

        String imageUrl = fileStorageService.uploadFile(file, "vehicles/" + vehicleId);
        VehicleImage image = new VehicleImage();
        image.setVehicle(vehicle);
        image.setImageUrl(imageUrl);
        image.setImageName(name != null ? name : file.getOriginalFilename());
        image.setSortOrder(sort);
        vehicle.getImages().add(image);
        vehicleModelRepository.save(vehicle);
        return ResponseEntity.ok(image);
    }

    @PutMapping("/{vehicleId}/images/{imageId}")
    @Transactional
    public ResponseEntity<VehicleImage> updateImage(@PathVariable Long vehicleId, @PathVariable Long imageId, @RequestBody AdminVehicleImageUpdateRequest request) {
        if (request == null) return ResponseEntity.badRequest().build();
        VehicleImage img = vehicleImageRepository.findById(imageId).orElse(null);
        if (img == null || img.getVehicle() == null || !vehicleId.equals(img.getVehicle().getId())) {
            return ResponseEntity.notFound().build();
        }
        if (request.getImageName() != null) img.setImageName(trimOrNull(request.getImageName()));
        if (request.getSortOrder() != null) img.setSortOrder(request.getSortOrder());
        return ResponseEntity.ok(vehicleImageRepository.save(img));
    }

    @DeleteMapping("/{vehicleId}/images/{imageId}")
    @Transactional
    public ResponseEntity<Void> deleteImage(@PathVariable Long vehicleId, @PathVariable Long imageId) {
        VehicleImage img = vehicleImageRepository.findById(imageId).orElse(null);
        if (img == null || img.getVehicle() == null || !vehicleId.equals(img.getVehicle().getId())) {
            return ResponseEntity.notFound().build();
        }
        vehicleImageRepository.delete(img);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{vehicleId}/documents")
    @Transactional
    public ResponseEntity<VehicleDocument> createDocument(@PathVariable Long vehicleId, @RequestBody VehicleDocumentUpsertRequest request) {
        if (request == null || isBlank(request.getDocUrl())) {
            return ResponseEntity.badRequest().build();
        }
        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            VehicleDocument doc = new VehicleDocument();
            doc.setVehicle(vehicle);
            doc.setDocType(trimOrNull(request.getDocType()));
            doc.setDocName(trimOrNull(request.getDocName()));
            doc.setDocUrl(trimOrNull(request.getDocUrl()));
            doc.setSha256(trimOrNull(request.getSha256()));
            doc.setSourceUrl(trimOrNull(request.getSourceUrl()));
            doc.setFetchedAt(request.getFetchedAt());
            VehicleDocument saved = vehicleDocumentRepository.save(doc);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{vehicleId}/documents/{docId}")
    @Transactional
    public ResponseEntity<Void> deleteDocument(@PathVariable Long vehicleId, @PathVariable Long docId) {
        VehicleDocument doc = vehicleDocumentRepository.findById(docId).orElse(null);
        if (doc == null || doc.getVehicle() == null || !vehicleId.equals(doc.getVehicle().getId())) {
            return ResponseEntity.notFound().build();
        }
        vehicleDocumentRepository.delete(doc);
        return ResponseEntity.noContent().build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

