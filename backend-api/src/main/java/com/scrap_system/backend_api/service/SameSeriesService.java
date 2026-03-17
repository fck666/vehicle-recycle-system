package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.dto.SameSeriesCandidateDto;
import com.scrap_system.backend_api.dto.SameSeriesResponse;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SameSeriesService {

    private final VehicleModelRepository vehicleModelRepository;

    @Transactional(readOnly = true)
    public Optional<SameSeriesResponse> findSameSeries(Long vehicleId, int yearWindow, int limit) {
        Optional<VehicleModel> targetOpt = vehicleModelRepository.findById(vehicleId);
        if (targetOpt.isEmpty()) {
            return Optional.empty();
        }
        VehicleModel target = targetOpt.get();
        int minYear = target.getModelYear() - yearWindow;
        int maxYear = target.getModelYear() + yearWindow;
        List<VehicleModel> pool = vehicleModelRepository.findSameSeriesPool(
                target.getId(),
                target.getManufacturerName(),
                target.getVehicleType(),
                target.getFuelType(),
                minYear,
                maxYear
        );

        String targetSeries = extractSeriesName(target);
        String targetModelPrefix = modelPrefix(target.getModel(), 6);
        List<SameSeriesCandidateDto> candidates = new ArrayList<>();
        for (VehicleModel c : pool) {
            ScoreResult scoreResult = score(target, c, targetSeries, targetModelPrefix);
            if (scoreResult.score < 58) {
                continue;
            }
            candidates.add(new SameSeriesCandidateDto(
                    c.getId(),
                    c.getBrand(),
                    c.getModel(),
                    c.getModelYear(),
                    c.getManufacturerName(),
                    c.getVehicleType(),
                    c.getFuelType(),
                    c.getCurbWeight(),
                    c.getWheelbaseMm(),
                    extractSeriesName(c),
                    scoreResult.score,
                    scoreResult.confidenceLevel,
                    scoreResult.reasons
            ));
        }

        candidates.sort(Comparator
                .comparing(SameSeriesCandidateDto::getScore, Comparator.reverseOrder())
                .thenComparing(SameSeriesCandidateDto::getModelYear, Comparator.reverseOrder())
                .thenComparing(SameSeriesCandidateDto::getVehicleId, Comparator.reverseOrder()));
        if (candidates.size() > limit) {
            candidates = new ArrayList<>(candidates.subList(0, limit));
        }

        int high = 0;
        int medium = 0;
        for (SameSeriesCandidateDto c : candidates) {
            if ("HIGH".equals(c.getConfidenceLevel())) {
                high++;
            } else if ("MEDIUM".equals(c.getConfidenceLevel())) {
                medium++;
            }
        }

        return Optional.of(new SameSeriesResponse(
                target.getId(),
                targetSeries,
                yearWindow,
                candidates.size(),
                high,
                medium,
                candidates
        ));
    }

    private ScoreResult score(VehicleModel target, VehicleModel candidate, String targetSeries, String targetModelPrefix) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        String candidateSeries = extractSeriesName(candidate);
        if (!targetSeries.isBlank() && targetSeries.equals(candidateSeries)) {
            score += 35;
            reasons.add("车系名称一致");
        } else if (!targetSeries.isBlank() && !candidateSeries.isBlank()
                && (targetSeries.contains(candidateSeries) || candidateSeries.contains(targetSeries))) {
            score += 25;
            reasons.add("车系名称高相似");
        }

        String candidatePrefix = modelPrefix(candidate.getModel(), 6);
        if (!targetModelPrefix.isBlank() && targetModelPrefix.equals(candidatePrefix)) {
            score += 25;
            reasons.add("公告型号前缀一致");
        } else if (!targetModelPrefix.isBlank()
                && targetModelPrefix.length() >= 5
                && candidatePrefix.length() >= 5
                && targetModelPrefix.substring(0, 5).equals(candidatePrefix.substring(0, 5))) {
            score += 18;
            reasons.add("公告型号前缀高相似");
        }

        if (equalsIgnoreCase(target.getManufacturerName(), candidate.getManufacturerName())) {
            score += 15;
            reasons.add("生产企业一致");
        }
        if (equalsIgnoreCase(target.getVehicleType(), candidate.getVehicleType())) {
            score += 8;
            reasons.add("车辆类型一致");
        }
        if (equalsIgnoreCase(target.getFuelType(), candidate.getFuelType())) {
            score += 8;
            reasons.add("能源类型一致");
        }

        if (target.getModelYear() != null && candidate.getModelYear() != null) {
            int yearDiff = Math.abs(target.getModelYear() - candidate.getModelYear());
            if (yearDiff <= 1) {
                score += 8;
                reasons.add("年款非常接近");
            } else if (yearDiff <= 3) {
                score += 5;
                reasons.add("年款接近");
            } else if (yearDiff <= 5) {
                score += 2;
                reasons.add("年款可接受");
            }
        }

        int weightBonus = closeBonus(target.getCurbWeight(), candidate.getCurbWeight(), new BigDecimal("60"), new BigDecimal("120"), 8, 4);
        if (weightBonus > 0) {
            score += weightBonus;
            reasons.add("整备质量接近");
        }

        int wheelbaseBonus = closeBonus(target.getWheelbaseMm(), candidate.getWheelbaseMm(), 20, 50, 8, 4);
        if (wheelbaseBonus > 0) {
            score += wheelbaseBonus;
            reasons.add("轴距接近");
        }

        String confidenceLevel = score >= 78 ? "HIGH" : (score >= 58 ? "MEDIUM" : "LOW");
        return new ScoreResult(score, confidenceLevel, reasons);
    }

    private static int closeBonus(BigDecimal a, BigDecimal b, BigDecimal strictTol, BigDecimal looseTol, int strictScore, int looseScore) {
        if (a == null || b == null) {
            return 0;
        }
        BigDecimal diff = a.subtract(b).abs();
        if (diff.compareTo(strictTol) <= 0) {
            return strictScore;
        }
        if (diff.compareTo(looseTol) <= 0) {
            return looseScore;
        }
        return 0;
    }

    private static int closeBonus(Integer a, Integer b, int strictTol, int looseTol, int strictScore, int looseScore) {
        if (a == null || b == null) {
            return 0;
        }
        int diff = Math.abs(a - b);
        if (diff <= strictTol) {
            return strictScore;
        }
        if (diff <= looseTol) {
            return looseScore;
        }
        return 0;
    }

    private static String extractSeriesName(VehicleModel vehicle) {
        String trademark = normalizeSeriesPart(vehicle.getTrademark());
        if (!trademark.isBlank()) {
            return trademark;
        }
        String brand = normalizeSeriesPart(vehicle.getBrand());
        if (!brand.isBlank()) {
            return brand;
        }
        String modelPrefix = modelPrefix(vehicle.getModel(), 6);
        return modelPrefix;
    }

    private static String normalizeSeriesPart(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        int idx = s.indexOf("(");
        if (idx > 0) {
            s = s.substring(0, idx);
        }
        s = s.replace("（", "(");
        idx = s.indexOf("(");
        if (idx > 0) {
            s = s.substring(0, idx);
        }
        s = s.replace("牌", "");
        s = s.replaceAll("\\s+", "");
        s = s.replaceAll("[^0-9a-zA-Z\\u4e00-\\u9fa5]+", "");
        return s.toLowerCase(Locale.ROOT);
    }

    private static String modelPrefix(String model, int len) {
        if (model == null) {
            return "";
        }
        String cleaned = model.trim().toUpperCase(Locale.ROOT).replaceAll("[^0-9A-Z]+", "");
        if (cleaned.isEmpty()) {
            return "";
        }
        return cleaned.length() <= len ? cleaned : cleaned.substring(0, len);
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private record ScoreResult(int score, String confidenceLevel, List<String> reasons) {
    }
}
