package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.VehicleDismantleRecord;
import com.scrap_system.backend_api.repository.VehicleDismantleRecordRepository;
import com.scrap_system.backend_api.repository.UserAccountRepository;
import com.scrap_system.backend_api.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicle-dismantle")
@RequiredArgsConstructor
public class AdminVehicleDismantleController {

    private final VehicleDismantleRecordRepository dismantleRecordRepository;
    private final UserAccountRepository userAccountRepository;

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleDismantleRecord>> listByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(dismantleRecordRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId));
    }

    @PostMapping
    public ResponseEntity<VehicleDismantleRecord> create(@RequestBody VehicleDismantleRecord record) {
        if (record.getVehicleId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            Long userId = (Long) authentication.getPrincipal();
            record.setOperatorId(String.valueOf(userId));
            userAccountRepository.findById(userId).ifPresent(u -> record.setOperatorName(u.getUsername()));
        }
        
        return ResponseEntity.ok(dismantleRecordRepository.save(record));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dismantleRecordRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
