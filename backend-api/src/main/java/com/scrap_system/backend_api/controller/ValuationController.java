package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.ValuationResult;
import com.scrap_system.backend_api.model.ValuationRecord;
import com.scrap_system.backend_api.service.ValuationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/valuation")
@RequiredArgsConstructor
@Slf4j
public class ValuationController {

    private final ValuationService valuationService;

    @PostMapping("/{vehicleId}")
    public ResponseEntity<?> calculate(@PathVariable Long vehicleId) {
        try {
            ValuationResult result = valuationService.calculateValuation(vehicleId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Valuation unexpected error, vehicleId={}", vehicleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "valuation failed"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ValuationRecord>> getHistory(@RequestParam Long vehicleId) {
        List<ValuationRecord> history = valuationService.getHistory(vehicleId);
        return ResponseEntity.ok(history);
    }
}
