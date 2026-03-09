package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.VehicleDismantleRecord;
import com.scrap_system.backend_api.repository.VehicleDismantleRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicle-dismantle")
@RequiredArgsConstructor
public class AdminVehicleDismantleController {

    private final VehicleDismantleRecordRepository dismantleRecordRepository;

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleDismantleRecord>> listByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(dismantleRecordRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId));
    }

    @PostMapping
    public ResponseEntity<VehicleDismantleRecord> create(@RequestBody VehicleDismantleRecord record) {
        if (record.getVehicleId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(dismantleRecordRepository.save(record));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dismantleRecordRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
