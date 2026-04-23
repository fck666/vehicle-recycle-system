package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.exception.ContentSecurityException;
import com.scrap_system.backend_api.model.VehicleDismantleRecord;
import com.scrap_system.backend_api.repository.VehicleDismantleRecordRepository;
import com.scrap_system.backend_api.repository.UserAccountRepository;
import com.scrap_system.backend_api.service.MiniProgramContentSecurityService;
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
    private final MiniProgramContentSecurityService contentSecurityService;

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleDismantleRecord>> listByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(dismantleRecordRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDismantleRecord> getById(@PathVariable Long id) {
        return dismantleRecordRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody VehicleDismantleRecord record) {
        if (record.getVehicleId() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            contentSecurityService.validateDismantleRecord(record);
        } catch (ContentSecurityException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            Long userId = (Long) authentication.getPrincipal();
            record.setOperatorId(String.valueOf(userId));
            userAccountRepository.findById(userId).ifPresent(u -> record.setOperatorName(u.getUsername()));
        }
        
        return ResponseEntity.ok(dismantleRecordRepository.save(record));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody VehicleDismantleRecord record) {
        return dismantleRecordRepository.findById(id).map(existing -> {
            try {
                contentSecurityService.validateDismantleRecord(record);
            } catch (ContentSecurityException ex) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
            }

            existing.setSteelWeight(record.getSteelWeight());
            existing.setAluminumWeight(record.getAluminumWeight());
            existing.setCopperWeight(record.getCopperWeight());
            existing.setBatteryWeight(record.getBatteryWeight());
            existing.setOtherWeight(record.getOtherWeight());
            existing.setDetailsJson(record.getDetailsJson());
            existing.setRemark(record.getRemark());
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Long) {
                Long userId = (Long) authentication.getPrincipal();
                existing.setOperatorId(String.valueOf(userId));
                userAccountRepository.findById(userId).ifPresent(u -> existing.setOperatorName(u.getUsername()));
            }
            return ResponseEntity.ok(dismantleRecordRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dismantleRecordRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
