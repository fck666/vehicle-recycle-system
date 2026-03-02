package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.BatchUpsertResult;
import com.scrap_system.backend_api.dto.MaterialPriceBatchUpsertRequest;
import com.scrap_system.backend_api.dto.MaterialPriceUpsertRequest;
import com.scrap_system.backend_api.model.JobRun;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import com.scrap_system.backend_api.service.JobRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/material-prices")
@RequiredArgsConstructor
public class MaterialPriceController {

    private final MaterialPriceRepository materialPriceRepository;
    private final JobRunService jobRunService;

    @GetMapping
    public List<MaterialPrice> list() {
        return materialPriceRepository.findLatestPerType();
    }

    @GetMapping("/{type}")
    public ResponseEntity<MaterialPrice> getByType(@PathVariable String type) {
        if (isBlank(type)) {
            return ResponseEntity.badRequest().build();
        }
        return materialPriceRepository.findFirstByTypeOrderByEffectiveDateDescFetchedAtDesc(type.trim())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{type}/history")
    public ResponseEntity<List<MaterialPrice>> history(
            @PathVariable String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        if (isBlank(type)) return ResponseEntity.badRequest().build();
        String t = type.trim();
        LocalDate toDate = isBlank(to) ? LocalDate.now() : LocalDate.parse(to.trim());
        LocalDate fromDate = isBlank(from) ? toDate.minusDays(90) : LocalDate.parse(from.trim());
        if (fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(materialPriceRepository.findByTypeAndEffectiveDateBetweenOrderByEffectiveDateDesc(t, fromDate, toDate));
    }

    @PostMapping
    public ResponseEntity<MaterialPrice> upsert(@RequestBody MaterialPriceUpsertRequest request) {
        if (request == null || isBlank(request.getType()) || request.getPricePerKg() == null) {
            return ResponseEntity.badRequest().build();
        }

        String type = request.getType().trim();
        LocalDate effectiveDate = request.getEffectiveDate();
        if (effectiveDate == null) {
            effectiveDate = request.getFetchedAt() == null ? LocalDate.now() : request.getFetchedAt().toLocalDate();
        }
        Optional<MaterialPrice> existing = materialPriceRepository.findByTypeAndEffectiveDate(type, effectiveDate);
        MaterialPrice p = existing.orElseGet(MaterialPrice::new);
        p.setType(type);
        p.setPricePerKg(request.getPricePerKg());
        if (!isBlank(request.getCurrency())) p.setCurrency(request.getCurrency().trim());
        if (!isBlank(request.getUnit())) p.setUnit(request.getUnit().trim());
        p.setEffectiveDate(effectiveDate);
        if (request.getFetchedAt() != null) p.setFetchedAt(request.getFetchedAt());
        if (!isBlank(request.getSourceName())) p.setSourceName(request.getSourceName().trim());
        if (!isBlank(request.getSourceUrl())) p.setSourceUrl(request.getSourceUrl().trim());
        if (!isBlank(request.getRawPayload())) p.setRawPayload(request.getRawPayload());

        return ResponseEntity.ok(materialPriceRepository.save(p));
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchUpsertResult> batchUpsert(
            @RequestBody MaterialPriceBatchUpsertRequest request,
            @RequestHeader(value = "X-Run-Id", required = false) String runId,
            Authentication authentication
    ) {
        Long userId = authentication != null && authentication.getPrincipal() instanceof Long ? (Long) authentication.getPrincipal() : null;
        JobRun jr = jobRunService.start("PRICE_BATCH_UPSERT", runId, userId, userId == null ? null : ("user:" + userId),
                request == null || request.getItems() == null ? null : ("{\"count\":" + request.getItems().size() + "}"));
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            jobRunService.success(jr, 0, 0, 0, "empty", null);
            return ResponseEntity.ok(new BatchUpsertResult(0, 0, 0));
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        try {
            for (MaterialPriceUpsertRequest item : request.getItems()) {
                if (item == null || isBlank(item.getType()) || item.getPricePerKg() == null) {
                    skipped++;
                    continue;
                }

                String type = item.getType().trim();
                LocalDate effectiveDate = item.getEffectiveDate();
                if (effectiveDate == null) {
                    effectiveDate = item.getFetchedAt() == null ? LocalDate.now() : item.getFetchedAt().toLocalDate();
                }
                Optional<MaterialPrice> existing = materialPriceRepository.findByTypeAndEffectiveDate(type, effectiveDate);
                MaterialPrice p = existing.orElseGet(MaterialPrice::new);
                boolean isInsert = existing.isEmpty();
                p.setType(type);
                p.setPricePerKg(item.getPricePerKg());
                if (!isBlank(item.getCurrency())) p.setCurrency(item.getCurrency().trim());
                if (!isBlank(item.getUnit())) p.setUnit(item.getUnit().trim());
                p.setEffectiveDate(effectiveDate);
                if (item.getFetchedAt() != null) p.setFetchedAt(item.getFetchedAt());
                if (!isBlank(item.getSourceName())) p.setSourceName(item.getSourceName().trim());
                if (!isBlank(item.getSourceUrl())) p.setSourceUrl(item.getSourceUrl().trim());
                if (!isBlank(item.getRawPayload())) p.setRawPayload(item.getRawPayload());
                materialPriceRepository.save(p);

                if (isInsert) inserted++;
                else updated++;
            }
            jobRunService.success(jr, inserted, updated, skipped, null, null);
        } catch (Exception e) {
            jobRunService.failed(jr, e.getMessage(), null);
            throw e;
        }

        return ResponseEntity.ok(new BatchUpsertResult(inserted, updated, skipped));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<Void> deleteByType(@PathVariable String type) {
        if (isBlank(type)) return ResponseEntity.badRequest().build();
        materialPriceRepository.deleteByType(type.trim());
        return ResponseEntity.noContent().build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
