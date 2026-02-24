package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.MaterialTemplateUpsertRequest;
import com.scrap_system.backend_api.model.MaterialTemplate;
import com.scrap_system.backend_api.repository.MaterialTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/material-templates")
@RequiredArgsConstructor
public class MaterialTemplateController {

    private final MaterialTemplateRepository materialTemplateRepository;

    @GetMapping
    public List<MaterialTemplate> list() {
        return materialTemplateRepository.findAll();
    }

    @GetMapping("/{vehicleType}")
    public ResponseEntity<MaterialTemplate> getByVehicleType(@PathVariable String vehicleType) {
        return materialTemplateRepository.findByVehicleType(vehicleType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MaterialTemplate> upsert(@RequestBody MaterialTemplateUpsertRequest request) {
        if (request == null || isBlank(request.getVehicleType())) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getSteelRatio() == null || request.getAluminumRatio() == null || request.getCopperRatio() == null || request.getRecoveryRatio() == null) {
            return ResponseEntity.badRequest().build();
        }

        String vehicleType = request.getVehicleType().trim();
        Optional<MaterialTemplate> existing = materialTemplateRepository.findByVehicleType(vehicleType);
        MaterialTemplate t = existing.orElseGet(MaterialTemplate::new);
        t.setVehicleType(vehicleType);
        t.setSteelRatio(request.getSteelRatio());
        t.setAluminumRatio(request.getAluminumRatio());
        t.setCopperRatio(request.getCopperRatio());
        t.setRecoveryRatio(request.getRecoveryRatio());

        MaterialTemplate saved = materialTemplateRepository.save(t);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{vehicleType}")
    public ResponseEntity<Void> deleteByVehicleType(@PathVariable String vehicleType) {
        Optional<MaterialTemplate> existing = materialTemplateRepository.findByVehicleType(vehicleType);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        materialTemplateRepository.delete(existing.get());
        return ResponseEntity.noContent().build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

