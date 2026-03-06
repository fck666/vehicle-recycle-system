package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.VehicleUpsertRequest;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.model.enums.VehicleSourceType;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/vehicles")
@RequiredArgsConstructor
public class AdminVehicleController {

    private final VehicleModelRepository vehicleModelRepository;
    private final com.scrap_system.backend_api.service.FileStorageService fileStorageService;

    @GetMapping("/lookup")
    public ResponseEntity<VehicleModel> lookup(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String productNo
    ) {
        if (!isBlank(productId)) {
            return vehicleModelRepository.findByProductId(productId.trim())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        if (!isBlank(productNo)) {
            return vehicleModelRepository.findByProductNo(productNo.trim())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping
    public ResponseEntity<Page<VehicleModel>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) List<String> manufacturers,
            @RequestParam(required = false) List<String> vehicleTypes,
            @RequestParam(required = false) List<String> fuelTypes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));

        Specification<VehicleModel> spec = com.scrap_system.backend_api.specification.VehicleSpecs.withDynamicQuery(
                q, brands, manufacturers, vehicleTypes, fuelTypes, null
        );

        return ResponseEntity.ok(vehicleModelRepository.findAll(spec, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleModel> get(@PathVariable Long id) {
        return vehicleModelRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VehicleModel> create(@RequestBody VehicleUpsertRequest request) {
        if (!hasRequiredForUpsert(request)) {
            return ResponseEntity.badRequest().build();
        }
        VehicleModel v = new VehicleModel();
        apply(v, request, true);
        return ResponseEntity.ok(vehicleModelRepository.save(v));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleModel> update(@PathVariable Long id, @RequestBody VehicleUpsertRequest request) {
        Optional<VehicleModel> existing = vehicleModelRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        VehicleModel v = existing.get();
        apply(v, request, false);
        if (!hasRequiredEntity(v)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(vehicleModelRepository.save(v));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<VehicleModel> existing = vehicleModelRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        VehicleModel v = existing.get();
        
        // Delete physical files
        if (v.getImages() != null) {
            for (com.scrap_system.backend_api.model.VehicleImage img : v.getImages()) {
                fileStorageService.deleteFile(img.getImageUrl());
            }
        }
        if (v.getDocuments() != null) {
            for (com.scrap_system.backend_api.model.VehicleDocument doc : v.getDocuments()) {
                fileStorageService.deleteFile(doc.getDocUrl());
            }
        }
        
        vehicleModelRepository.delete(v);
        return ResponseEntity.noContent().build();
    }

    private static void apply(VehicleModel v, VehicleUpsertRequest r, boolean isCreate) {
        if (r.getSourceType() != null) {
            v.setSourceType(r.getSourceType());
        } else if (isCreate) {
            v.setSourceType(VehicleSourceType.MANUAL);
        } else {
            // Updating existing record: if it was CRAWLED, mark as EDITED
            if (v.getSourceType() == VehicleSourceType.CRAWLED) {
                v.setSourceType(VehicleSourceType.EDITED);
            }
        }

        if (!isBlank(r.getBrand()) || isCreate) v.setBrand(trimOrNull(r.getBrand()));
        if (!isBlank(r.getModel()) || isCreate) v.setModel(trimOrNull(r.getModel()));
        if (r.getModelYear() != null || isCreate) v.setModelYear(r.getModelYear());
        if (!isBlank(r.getFuelType()) || isCreate) v.setFuelType(trimOrNull(r.getFuelType()));
        if (!isBlank(r.getVehicleType()) || isCreate) v.setVehicleType(trimOrNull(r.getVehicleType()));
        if (r.getCurbWeight() != null || isCreate) v.setCurbWeight(r.getCurbWeight());

        if (r.getBatteryKwh() != null) v.setBatteryKwh(r.getBatteryKwh());
        if (r.getProductNo() != null) v.setProductNo(trimOrNull(r.getProductNo()));
        if (r.getProductId() != null) v.setProductId(trimOrNull(r.getProductId()));
        if (r.getProductModel() != null) v.setProductModel(trimOrNull(r.getProductModel()));
        if (r.getManufacturerName() != null) v.setManufacturerName(trimOrNull(r.getManufacturerName()));
        if (r.getTrademark() != null) v.setTrademark(trimOrNull(r.getTrademark()));
        if (r.getProductionAddress() != null) v.setProductionAddress(trimOrNull(r.getProductionAddress()));
        if (r.getRegistrationAddress() != null) v.setRegistrationAddress(trimOrNull(r.getRegistrationAddress()));
        if (r.getReleaseDate() != null) v.setReleaseDate(r.getReleaseDate());
        if (r.getEffectiveDate() != null) v.setEffectiveDate(r.getEffectiveDate());
        if (r.getBatchNo() != null) v.setBatchNo(r.getBatchNo());
        if (r.getCatalogIndex() != null) v.setCatalogIndex(r.getCatalogIndex());

        if (r.getLengthMm() != null) v.setLengthMm(r.getLengthMm());
        if (r.getWidthMm() != null) v.setWidthMm(r.getWidthMm());
        if (r.getHeightMm() != null) v.setHeightMm(r.getHeightMm());
        if (r.getWheelbaseMm() != null) v.setWheelbaseMm(r.getWheelbaseMm());
        if (r.getFrontOverhangMm() != null) v.setFrontOverhangMm(r.getFrontOverhangMm());
        if (r.getRearOverhangMm() != null) v.setRearOverhangMm(r.getRearOverhangMm());
        if (r.getApproachAngleDeg() != null) v.setApproachAngleDeg(r.getApproachAngleDeg());
        if (r.getDepartureAngleDeg() != null) v.setDepartureAngleDeg(r.getDepartureAngleDeg());
        if (r.getFrontTrackMm() != null) v.setFrontTrackMm(r.getFrontTrackMm());
        if (r.getRearTrackMm() != null) v.setRearTrackMm(r.getRearTrackMm());
        if (r.getAxleCount() != null) v.setAxleCount(r.getAxleCount());
        if (r.getAxleLoadKg() != null) v.setAxleLoadKg(trimOrNull(r.getAxleLoadKg()));
        if (r.getTireCount() != null) v.setTireCount(r.getTireCount());
        if (r.getTireSpec() != null) v.setTireSpec(trimOrNull(r.getTireSpec()));
        if (r.getSteeringType() != null) v.setSteeringType(trimOrNull(r.getSteeringType()));
        if (r.getHasAbs() != null) v.setHasAbs(r.getHasAbs());
        if (r.getMaxSpeedKmh() != null) v.setMaxSpeedKmh(r.getMaxSpeedKmh());
        if (r.getGrossWeight() != null) v.setGrossWeight(r.getGrossWeight());
        if (r.getMotorModel() != null) v.setMotorModel(trimOrNull(r.getMotorModel()));
        if (r.getMotorManufacturer() != null) v.setMotorManufacturer(trimOrNull(r.getMotorManufacturer()));
        if (r.getDisplacementMl() != null) v.setDisplacementMl(r.getDisplacementMl());
        if (r.getPowerKw() != null) v.setPowerKw(r.getPowerKw());
        if (r.getVinPattern() != null) v.setVinPattern(trimOrNull(r.getVinPattern()));
        if (r.getChassisModel() != null) v.setChassisModel(trimOrNull(r.getChassisModel()));
        if (r.getSourceSite() != null) v.setSourceSite(trimOrNull(r.getSourceSite()));
        if (r.getSpecRawJson() != null) v.setSpecRawJson(r.getSpecRawJson());
    }

    private static boolean hasRequiredForUpsert(VehicleUpsertRequest r) {
        if (r == null) return false;
        if (isBlank(r.getBrand())) return false;
        if (isBlank(r.getModel())) return false;
        if (r.getModelYear() == null) return false;
        if (isBlank(r.getFuelType())) return false;
        if (isBlank(r.getVehicleType())) return false;
        return r.getCurbWeight() != null && r.getCurbWeight().compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean hasRequiredEntity(VehicleModel v) {
        if (v == null) return false;
        if (isBlank(v.getBrand())) return false;
        if (isBlank(v.getModel())) return false;
        if (v.getModelYear() == null) return false;
        if (isBlank(v.getFuelType())) return false;
        if (isBlank(v.getVehicleType())) return false;
        return v.getCurbWeight() != null && v.getCurbWeight().compareTo(BigDecimal.ZERO) > 0;
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
