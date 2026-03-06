package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.BatchUpsertResult;
import com.scrap_system.backend_api.dto.VehicleSpecBatchUpsertRequest;
import com.scrap_system.backend_api.dto.VehicleSpecUpsertItem;
import com.scrap_system.backend_api.model.JobRun;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.model.enums.VehicleSourceType;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.service.JobRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicle-specs")
@RequiredArgsConstructor
public class VehicleSpecController {

    private final VehicleModelRepository vehicleModelRepository;
    private final JobRunService jobRunService;

    @PostMapping("/batch")
    public ResponseEntity<BatchUpsertResult> batchUpsert(
            @RequestBody VehicleSpecBatchUpsertRequest request,
            @RequestHeader(value = "X-Run-Id", required = false) String runId,
            Authentication authentication
    ) {
        Long userId = authentication != null && authentication.getPrincipal() instanceof Long ? (Long) authentication.getPrincipal() : null;
        JobRun jr = jobRunService.start("MIIT_VEHICLE_SPECS_UPSERT", runId, userId, userId == null ? null : ("user:" + userId),
                request == null || request.getItems() == null ? null : ("{\"count\":" + request.getItems().size() + "}"));
        try {
            if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
                jobRunService.success(jr, 0, 0, 0, "empty", null);
                return ResponseEntity.ok(new BatchUpsertResult(0, 0, 0));
            }

            int inserted = 0;
            int updated = 0;
            int skipped = 0;

            for (VehicleSpecUpsertItem item : request.getItems()) {
                if (item == null) {
                    skipped++;
                    continue;
                }

                String productId = normalize(item.getProductId());
                String productNo = normalize(item.getProductNo());
                if (isBlank(productId) && isBlank(productNo)) {
                    skipped++;
                    continue;
                }

                Optional<VehicleModel> existing = !isBlank(productId)
                        ? vehicleModelRepository.findByProductId(productId)
                        : vehicleModelRepository.findByProductNo(productNo);

                boolean isInsert = existing.isEmpty();
                VehicleModel model = existing.orElseGet(VehicleModel::new);
                if (isInsert || model.getSourceType() == null) {
                    model.setSourceType(VehicleSourceType.CRAWLED);
                }

                applyDefaults(model, item, productId, productNo);
                applyNonNull(model, item);

                vehicleModelRepository.save(model);

                if (isInsert) {
                    inserted++;
                } else {
                    updated++;
                }
            }

            jobRunService.success(jr, inserted, updated, skipped, null, null);
            return ResponseEntity.ok(new BatchUpsertResult(inserted, updated, skipped));
        } catch (Exception e) {
            jobRunService.failed(jr, e.getMessage(), null);
            throw e;
        }
    }

    private void applyDefaults(VehicleModel target, VehicleSpecUpsertItem item, String productId, String productNo) {
        if (target.getProductId() == null && !isBlank(productId)) {
            target.setProductId(productId);
        }
        if (target.getProductNo() == null && !isBlank(productNo)) {
            target.setProductNo(productNo);
        }

        if (isBlank(target.getBrand())) {
            String brand = firstNonBlank(item.getBrand(), item.getTrademark(), item.getManufacturerName(), "unknown");
            target.setBrand(brand);
        }
        if (isBlank(target.getModel())) {
            String m = firstNonBlank(item.getModel(), item.getProductModel(), item.getProductNo(), "unknown");
            target.setModel(m);
        }
        if (target.getModelYear() == null) {
            Integer year = item.getModelYear();
            if (year == null) {
                LocalDate d = firstNonNull(item.getReleaseDate(), item.getEffectiveDate());
                if (d != null) {
                    year = d.getYear();
                }
            }
            target.setModelYear(year != null ? year : 0);
        }
        if (isBlank(target.getFuelType())) {
            target.setFuelType(!isBlank(item.getFuelType()) ? item.getFuelType() : "unknown");
        }
        if (target.getCurbWeight() == null) {
            BigDecimal cw = item.getCurbWeight();
            target.setCurbWeight(cw != null ? cw : BigDecimal.ZERO);
        }
        if (isBlank(target.getVehicleType())) {
            target.setVehicleType(!isBlank(item.getVehicleType()) ? item.getVehicleType() : "unknown");
        }
        if (isBlank(target.getSourceSite())) {
            target.setSourceSite(!isBlank(item.getSourceSite()) ? item.getSourceSite() : "miit");
        }
    }

    private void applyNonNull(VehicleModel target, VehicleSpecUpsertItem item) {
        if (!isBlank(item.getBrand())) target.setBrand(item.getBrand());
        if (!isBlank(item.getModel())) target.setModel(item.getModel());
        if (item.getModelYear() != null) target.setModelYear(item.getModelYear());
        if (!isBlank(item.getFuelType())) target.setFuelType(item.getFuelType());
        if (item.getCurbWeight() != null) target.setCurbWeight(item.getCurbWeight());
        if (item.getBatteryKwh() != null) target.setBatteryKwh(item.getBatteryKwh());
        if (!isBlank(item.getVehicleType())) target.setVehicleType(item.getVehicleType());

        if (!isBlank(item.getProductNo())) target.setProductNo(item.getProductNo());
        if (!isBlank(item.getProductId())) target.setProductId(item.getProductId());
        if (!isBlank(item.getProductModel())) target.setProductModel(item.getProductModel());
        if (!isBlank(item.getManufacturerName())) target.setManufacturerName(item.getManufacturerName());
        if (!isBlank(item.getTrademark())) target.setTrademark(item.getTrademark());
        if (!isBlank(item.getProductionAddress())) target.setProductionAddress(item.getProductionAddress());
        if (!isBlank(item.getRegistrationAddress())) target.setRegistrationAddress(item.getRegistrationAddress());
        if (item.getReleaseDate() != null) target.setReleaseDate(item.getReleaseDate());
        if (item.getEffectiveDate() != null) target.setEffectiveDate(item.getEffectiveDate());
        if (item.getBatchNo() != null) target.setBatchNo(item.getBatchNo());
        if (item.getCatalogIndex() != null) target.setCatalogIndex(item.getCatalogIndex());

        if (item.getLengthMm() != null) target.setLengthMm(item.getLengthMm());
        if (item.getWidthMm() != null) target.setWidthMm(item.getWidthMm());
        if (item.getHeightMm() != null) target.setHeightMm(item.getHeightMm());
        if (item.getWheelbaseMm() != null) target.setWheelbaseMm(item.getWheelbaseMm());
        if (item.getFrontOverhangMm() != null) target.setFrontOverhangMm(item.getFrontOverhangMm());
        if (item.getRearOverhangMm() != null) target.setRearOverhangMm(item.getRearOverhangMm());
        if (item.getApproachAngleDeg() != null) target.setApproachAngleDeg(item.getApproachAngleDeg());
        if (item.getDepartureAngleDeg() != null) target.setDepartureAngleDeg(item.getDepartureAngleDeg());
        if (item.getFrontTrackMm() != null) target.setFrontTrackMm(item.getFrontTrackMm());
        if (item.getRearTrackMm() != null) target.setRearTrackMm(item.getRearTrackMm());

        if (item.getAxleCount() != null) target.setAxleCount(item.getAxleCount());
        if (!isBlank(item.getAxleLoadKg())) target.setAxleLoadKg(item.getAxleLoadKg());
        if (item.getTireCount() != null) target.setTireCount(item.getTireCount());
        if (!isBlank(item.getTireSpec())) target.setTireSpec(item.getTireSpec());
        if (!isBlank(item.getSteeringType())) target.setSteeringType(item.getSteeringType());
        if (item.getHasAbs() != null) target.setHasAbs(item.getHasAbs());
        if (item.getMaxSpeedKmh() != null) target.setMaxSpeedKmh(item.getMaxSpeedKmh());

        if (item.getGrossWeight() != null) target.setGrossWeight(item.getGrossWeight());

        if (!isBlank(item.getMotorModel())) target.setMotorModel(item.getMotorModel());
        if (!isBlank(item.getMotorManufacturer())) target.setMotorManufacturer(item.getMotorManufacturer());
        if (item.getDisplacementMl() != null) target.setDisplacementMl(item.getDisplacementMl());
        if (item.getPowerKw() != null) target.setPowerKw(item.getPowerKw());

        if (!isBlank(item.getVinPattern())) target.setVinPattern(item.getVinPattern());
        if (!isBlank(item.getChassisModel())) target.setChassisModel(item.getChassisModel());

        if (!isBlank(item.getSourceSite())) target.setSourceSite(item.getSourceSite());
        if (!isBlank(item.getSpecRawJson())) target.setSpecRawJson(item.getSpecRawJson());
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) return null;
        for (String s : candidates) {
            if (!isBlank(s)) return s;
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... candidates) {
        if (candidates == null) return null;
        for (T t : candidates) {
            if (t != null) return t;
        }
        return null;
    }
}
