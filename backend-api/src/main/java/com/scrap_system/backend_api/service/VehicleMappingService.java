package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.model.ExternalVehicleTrim;
import com.scrap_system.backend_api.model.VehicleMapping;
import com.scrap_system.backend_api.model.VehicleMappingCandidate;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.ExternalVehicleTrimRepository;
import com.scrap_system.backend_api.repository.VehicleMappingCandidateRepository;
import com.scrap_system.backend_api.repository.VehicleMappingRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VehicleMappingService {

    private static final String MATCHER_VERSION = "v1";

    private final VehicleModelRepository vehicleModelRepository;
    private final ExternalVehicleTrimRepository externalVehicleTrimRepository;
    private final VehicleMappingRepository vehicleMappingRepository;
    private final VehicleMappingCandidateRepository candidateRepository;

    @Transactional
    public void recomputeCandidates(Long miitVehicleId) {
        VehicleModel v = vehicleModelRepository.findById(miitVehicleId).orElse(null);
        if (v == null) return;

        candidateRepository.deleteByMiitVehicleId(miitVehicleId);

        String q = buildQuery(v);
        List<ExternalVehicleTrim> pool = externalVehicleTrimRepository
                .search(q.isBlank() ? null : q, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id")))
                .getContent();

        List<Scored> scored = new ArrayList<>();
        for (ExternalVehicleTrim t : pool) {
            double score = score(v, t);
            if (score <= 0.1) continue;
            scored.add(new Scored(t, score));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        int limit = Math.min(5, scored.size());
        for (int i = 0; i < limit; i++) {
            Scored s = scored.get(i);
            VehicleMappingCandidate c = new VehicleMappingCandidate();
            c.setMiitVehicleId(miitVehicleId);
            c.setExternalTrimId(s.trim.getId());
            c.setScore(s.score);
            c.setRankNo(i + 1);
            c.setMatchedBy(MATCHER_VERSION);
            candidateRepository.save(c);
        }

        autoConfirmIfStrong(miitVehicleId, scored);
    }

    @Transactional
    public void confirm(Long miitVehicleId, Long externalTrimId) {
        VehicleMapping m = vehicleMappingRepository.findByMiitVehicleId(miitVehicleId).orElseGet(VehicleMapping::new);
        m.setMiitVehicleId(miitVehicleId);
        m.setExternalTrimId(externalTrimId);
        m.setStatus("CONFIRMED");
        m.setScore(null);
        m.setMatchedBy("MANUAL");
        vehicleMappingRepository.save(m);
    }

    private void autoConfirmIfStrong(Long miitVehicleId, List<Scored> scored) {
        if (scored.isEmpty()) return;
        double s1 = scored.get(0).score;
        double s2 = scored.size() >= 2 ? scored.get(1).score : 0.0;
        if (s1 >= 0.92 && (s1 - s2) >= 0.10) {
            VehicleMapping m = vehicleMappingRepository.findByMiitVehicleId(miitVehicleId).orElseGet(VehicleMapping::new);
            m.setMiitVehicleId(miitVehicleId);
            m.setExternalTrimId(scored.get(0).trim.getId());
            m.setStatus("CONFIRMED");
            m.setScore(s1);
            m.setMatchedBy(MATCHER_VERSION);
            vehicleMappingRepository.save(m);
        } else {
            VehicleMapping m = vehicleMappingRepository.findByMiitVehicleId(miitVehicleId).orElseGet(VehicleMapping::new);
            if (m.getId() == null) {
                m.setMiitVehicleId(miitVehicleId);
                m.setStatus("SUGGESTED");
                m.setExternalTrimId(null);
                m.setScore(s1);
                m.setMatchedBy(MATCHER_VERSION);
                vehicleMappingRepository.save(m);
            } else if ("SUGGESTED".equals(m.getStatus())) {
                m.setScore(s1);
                m.setMatchedBy(MATCHER_VERSION);
                vehicleMappingRepository.save(m);
            }
        }
    }

    private static String buildQuery(VehicleModel v) {
        String b = safe(v.getBrand());
        String m = safe(v.getModel());
        String q = (b + " " + m).trim();
        return q.length() > 64 ? q.substring(0, 64) : q;
    }

    private static double score(VehicleModel v, ExternalVehicleTrim t) {
        String vn = normalize(safe(v.getBrand()) + safe(v.getModel()));
        String tn = normalize(safe(t.getBrand()) + safe(t.getSeriesName()) + safe(t.getMarketName()));

        double nameScore = diceCoefficientTrigram(vn, tn);
        if (!vn.isBlank() && tn.contains(vn)) nameScore = Math.max(nameScore, 0.85);
        if (!isBlank(v.getModel()) && normalize(safe(t.getMarketName())).contains(normalize(v.getModel()))) {
            nameScore = Math.max(nameScore, 0.65);
        }

        double s = 0.60 * nameScore;

        Integer vy = v.getModelYear();
        Integer ty = t.getModelYear();
        if (vy != null && ty != null) {
            int d = Math.abs(vy - ty);
            if (d == 0) s += 0.12;
            else if (d == 1) s += 0.06;
            else if (d >= 4) s -= 0.08;
        }

        String ve = normalizeEnergy(v.getFuelType());
        String te = normalizeEnergy(t.getEnergyType());
        if (!ve.isBlank() && !te.isBlank()) {
            if (ve.equals(te)) s += 0.10;
            else s -= 0.15;
        }

        s += numericCloseBonus(v.getBatteryKwh(), t.getBatteryKwh(), new BigDecimal("5"), 0.10);
        s += numericCloseBonus(v.getPowerKw(), t.getPowerKw(), new BigDecimal("10"), 0.06);
        s += numericCloseBonus(v.getCurbWeight(), t.getCurbWeight(), new BigDecimal("80"), 0.06);

        return clamp(s, 0.0, 1.0);
    }

    private static double numericCloseBonus(BigDecimal a, BigDecimal b, BigDecimal tol, double bonus) {
        if (a == null || b == null) return 0.0;
        BigDecimal diff = a.subtract(b).abs();
        if (diff.compareTo(tol) <= 0) return bonus;
        BigDecimal tol2 = tol.multiply(new BigDecimal("2"));
        if (diff.compareTo(tol2) <= 0) return bonus * 0.5;
        return -bonus * 0.5;
    }

    private static String normalizeEnergy(String fuelType) {
        String f = normalize(safe(fuelType));
        if (f.contains("ev") || f.contains("纯电") || f.contains("电动")) return "EV";
        if (f.contains("hybrid") || f.contains("混动") || f.contains("插混") || f.contains("phev")) return "HYBRID";
        if (f.contains("diesel") || f.contains("柴油")) return "DIESEL";
        if (!f.isBlank()) return "GAS";
        return "";
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replace("（", "(")
                .replace("）", ")")
                .replaceAll("[\\s\\-_/]+", "")
                .replaceAll("[^0-9a-z\\u4e00-\\u9fa5]+", "");
    }

    private static double diceCoefficientTrigram(String a, String b) {
        if (a.isBlank() || b.isBlank()) return 0.0;
        if (a.equals(b)) return 1.0;
        Set<String> ta = trigrams(a);
        Set<String> tb = trigrams(b);
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        int inter = 0;
        for (String x : ta) if (tb.contains(x)) inter++;
        return (2.0 * inter) / (ta.size() + tb.size());
    }

    private static Set<String> trigrams(String s) {
        Set<String> set = new HashSet<>();
        if (s.length() < 3) {
            set.add(s);
            return set;
        }
        for (int i = 0; i + 3 <= s.length(); i++) {
            set.add(s.substring(i, i + 3));
        }
        return set;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private record Scored(ExternalVehicleTrim trim, double score) {
    }
}

