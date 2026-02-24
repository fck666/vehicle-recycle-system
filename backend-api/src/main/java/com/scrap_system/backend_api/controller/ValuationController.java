package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.ValuationResult;
import com.scrap_system.backend_api.model.ValuationRecord;
import com.scrap_system.backend_api.service.ValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/valuation")
@RequiredArgsConstructor
public class ValuationController {

    private final ValuationService valuationService;

    @PostMapping("/{vehicleId}")
    public ResponseEntity<ValuationResult> calculate(@PathVariable Long vehicleId) {
        ValuationResult result = valuationService.calculateValuation(vehicleId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{vehicleId}/precise")
    public ResponseEntity<ValuationResult> calculatePrecise(@PathVariable Long vehicleId, @RequestBody com.scrap_system.backend_api.dto.PreciseValuationRequest request) {
        ValuationResult result = valuationService.calculatePreciseValuation(vehicleId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ValuationRecord>> getHistory(@RequestParam Long vehicleId) {
        List<ValuationRecord> history = valuationService.getHistory(vehicleId);
        return ResponseEntity.ok(history);
    }
}
