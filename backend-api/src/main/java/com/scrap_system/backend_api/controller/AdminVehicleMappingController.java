package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.VehicleMappingCandidateDto;
import com.scrap_system.backend_api.dto.VehicleMappingConfirmRequest;
import com.scrap_system.backend_api.dto.VehicleMappingRowDto;
import com.scrap_system.backend_api.model.ExternalVehicleTrim;
import com.scrap_system.backend_api.model.VehicleMapping;
import com.scrap_system.backend_api.model.VehicleMappingCandidate;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.ExternalVehicleTrimRepository;
import com.scrap_system.backend_api.repository.VehicleMappingCandidateRepository;
import com.scrap_system.backend_api.repository.VehicleMappingRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.service.VehicleMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/vehicle-mappings")
@RequiredArgsConstructor
public class AdminVehicleMappingController {

    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleMappingRepository mappingRepository;
    private final VehicleMappingCandidateRepository candidateRepository;
    private final ExternalVehicleTrimRepository trimRepository;
    private final VehicleMappingService vehicleMappingService;

    @GetMapping
    public ResponseEntity<Page<VehicleMappingRowDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        String query = isBlank(q) ? null : q.trim();
        Page<VehicleModel> pv = vehicleModelRepository.search(query, pageable);

        List<Long> vehicleIds = pv.getContent().stream().map(VehicleModel::getId).filter(Objects::nonNull).toList();
        Map<Long, VehicleMapping> map = new HashMap<>();
        for (VehicleMapping m : mappingRepository.findByMiitVehicleIdIn(vehicleIds)) {
            map.put(m.getMiitVehicleId(), m);
        }
        Map<Long, List<VehicleMappingCandidate>> candMap = new HashMap<>();
        for (VehicleMappingCandidate c : candidateRepository.findByMiitVehicleIdInOrderByMiitVehicleIdAscRankNoAsc(vehicleIds)) {
            candMap.computeIfAbsent(c.getMiitVehicleId(), k -> new ArrayList<>()).add(c);
        }

        Set<Long> trimIds = new HashSet<>();
        for (VehicleMapping m : map.values()) if (m.getExternalTrimId() != null) trimIds.add(m.getExternalTrimId());
        for (List<VehicleMappingCandidate> cs : candMap.values()) for (VehicleMappingCandidate c : cs) trimIds.add(c.getExternalTrimId());
        Map<Long, ExternalVehicleTrim> trimMap = new HashMap<>();
        if (!trimIds.isEmpty()) {
            for (ExternalVehicleTrim t : trimRepository.findAllById(trimIds)) {
                trimMap.put(t.getId(), t);
            }
        }

        List<VehicleMappingRowDto> rows = new ArrayList<>();
        for (VehicleModel v : pv.getContent()) {
            VehicleMapping m = map.get(v.getId());
            if (!isBlank(status)) {
                String s = status.trim().toUpperCase();
                String ms = m == null ? "UNMAPPED" : safe(m.getStatus());
                if (!s.equals(ms)) continue;
            }

            ExternalVehicleTrim ext = m != null && m.getExternalTrimId() != null ? trimMap.get(m.getExternalTrimId()) : null;
            List<VehicleMappingCandidateDto> cands = new ArrayList<>();
            for (VehicleMappingCandidate c : candMap.getOrDefault(v.getId(), List.of())) {
                ExternalVehicleTrim t = trimMap.get(c.getExternalTrimId());
                if (t == null) continue;
                cands.add(new VehicleMappingCandidateDto(
                        t.getId(),
                        t.getSource(),
                        t.getSourceTrimId(),
                        t.getBrand(),
                        t.getSeriesName(),
                        t.getMarketName(),
                        t.getModelYear(),
                        t.getEnergyType(),
                        t.getCoverUrl(),
                        t.getPageUrl(),
                        c.getScore(),
                        c.getRankNo()
                ));
            }
            String ms = m == null ? "UNMAPPED" : safe(m.getStatus());
            rows.add(new VehicleMappingRowDto(
                    v.getId(),
                    v.getBrand(),
                    v.getModel(),
                    v.getModelYear(),
                    v.getFuelType(),
                    v.getBatteryKwh(),
                    v.getCurbWeight(),
                    v.getProductId(),
                    v.getProductNo(),
                    ms,
                    ext == null ? null : ext.getId(),
                    ext == null ? null : ext.getMarketName(),
                    ext == null ? null : ext.getPageUrl(),
                    ext == null ? null : ext.getCoverUrl(),
                    cands
            ));
        }

        Page<VehicleMappingRowDto> out = new PageImpl<>(rows, pageable, isBlank(status) ? pv.getTotalElements() : rows.size());
        return ResponseEntity.ok(out);
    }

    @PostMapping("/{miitVehicleId}/recompute")
    public ResponseEntity<Void> recompute(@PathVariable Long miitVehicleId) {
        vehicleMappingService.recomputeCandidates(miitVehicleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{miitVehicleId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long miitVehicleId, @RequestBody VehicleMappingConfirmRequest request) {
        if (request == null || request.getExternalTrimId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!trimRepository.existsById(request.getExternalTrimId())) {
            return ResponseEntity.badRequest().build();
        }
        vehicleMappingService.confirm(miitVehicleId, request.getExternalTrimId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recompute")
    @Transactional
    public ResponseEntity<Void> recomputeBatch(@RequestParam(required = false) Integer limit) {
        int lim = limit == null ? 200 : Math.min(Math.max(limit, 1), 2000);
        List<VehicleModel> vs = vehicleModelRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        int done = 0;
        for (VehicleModel v : vs) {
            if (done >= lim) break;
            VehicleMapping m = mappingRepository.findByMiitVehicleId(v.getId()).orElse(null);
            if (m != null && "CONFIRMED".equals(m.getStatus())) continue;
            vehicleMappingService.recomputeCandidates(v.getId());
            done++;
        }
        return ResponseEntity.noContent().build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}

