package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.BatchUpsertResult;
import com.scrap_system.backend_api.dto.ExternalVehicleTrimBatchUpsertRequest;
import com.scrap_system.backend_api.dto.ExternalVehicleTrimUpsertRequest;
import com.scrap_system.backend_api.model.ExternalVehicleTrim;
import com.scrap_system.backend_api.repository.ExternalVehicleTrimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/external-trims")
@RequiredArgsConstructor
public class AdminExternalVehicleTrimController {

    private final ExternalVehicleTrimRepository repository;

    @GetMapping
    public ResponseEntity<Page<ExternalVehicleTrim>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        String query = isBlank(q) ? null : q.trim();
        return ResponseEntity.ok(repository.search(query, pageable));
    }

    @PostMapping("/batch")
    @Transactional
    public ResponseEntity<BatchUpsertResult> batchUpsert(@RequestBody ExternalVehicleTrimBatchUpsertRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.ok(new BatchUpsertResult(0, 0, 0));
        }
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        for (ExternalVehicleTrimUpsertRequest item : request.getItems()) {
            if (item == null || isBlank(item.getSource()) || isBlank(item.getSourceTrimId()) || isBlank(item.getBrand())) {
                skipped++;
                continue;
            }
            try {
                String source = item.getSource().trim().toUpperCase();
                String sourceTrimId = item.getSourceTrimId().trim();
                Optional<ExternalVehicleTrim> existing = repository.findBySourceAndSourceTrimId(source, sourceTrimId);
                ExternalVehicleTrim t = existing.orElseGet(ExternalVehicleTrim::new);
                t.setSource(source);
                t.setSourceTrimId(sourceTrimId);
                t.setBrand(item.getBrand().trim());
                t.setSeriesName(trimOrNull(item.getSeriesName()));
                t.setMarketName(trimOrNull(item.getMarketName()));
                t.setModelYear(item.getModelYear());
                t.setEnergyType(trimOrNull(item.getEnergyType()));
                t.setOfficialPrice(item.getOfficialPrice());
                t.setBatteryKwh(item.getBatteryKwh());
                t.setDisplacementMl(item.getDisplacementMl());
                t.setPowerKw(item.getPowerKw());
                t.setCurbWeight(item.getCurbWeight());
                t.setCoverUrl(trimOrNull(item.getCoverUrl()));
                t.setPageUrl(trimOrNull(item.getPageUrl()));
                t.setRawJson(item.getRawJson());
                repository.save(t);
                if (existing.isEmpty()) inserted++;
                else updated++;
            } catch (DataIntegrityViolationException e) {
                skipped++;
            }
        }
        return ResponseEntity.ok(new BatchUpsertResult(inserted, updated, skipped));
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

