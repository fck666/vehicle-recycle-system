package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.config.PerformanceLogSupport;
import com.scrap_system.backend_api.dto.SameSeriesCandidateDto;
import com.scrap_system.backend_api.dto.SameSeriesResponse;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.repository.projection.VehicleSeriesSnapshotView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class SameSeriesService {

    private static final AtomicBoolean INDEXED_FALLBACK_LOGGED = new AtomicBoolean(false);

    private final VehicleModelRepository vehicleModelRepository;
    private final PerformanceLogSupport performanceLogSupport;

    @Transactional(readOnly = true)
    public Optional<SameSeriesResponse> findSameSeries(Long vehicleId, int yearWindow, int limit) {
        long totalStart = System.nanoTime();

        long loadTargetStart = System.nanoTime();
        Optional<VehicleSeriesSnapshot> targetOpt = vehicleModelRepository.findSeriesSnapshotById(vehicleId)
                .map(VehicleSeriesSnapshot::fromView);
        long loadTargetMs = performanceLogSupport.elapsedMillis(loadTargetStart);
        if (targetOpt.isEmpty()) {
            return Optional.empty();
        }
        VehicleSeriesSnapshot target = targetOpt.get();
        int minYear = target.modelYear() - yearWindow;
        int maxYear = target.modelYear() + yearWindow;

        long poolQueryStart = System.nanoTime();
        boolean indexedQuery = true;
        List<VehicleSeriesSnapshot> pool;
        try {
            pool = vehicleModelRepository.findSameSeriesPoolSnapshotsIndexed(
                    target.id(),
                    normalizeQueryKey(target.manufacturerName()),
                    normalizeQueryKey(target.vehicleType()),
                    normalizeQueryKey(target.fuelType()),
                    minYear,
                    maxYear
            ).stream().map(VehicleSeriesSnapshot::fromView).toList();
        } catch (RuntimeException e) {
            indexedQuery = false;
            if (INDEXED_FALLBACK_LOGGED.compareAndSet(false, true)) {
                log.warn("Same series indexed query unavailable, fallback enabled: {}: {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
            pool = vehicleModelRepository.findSameSeriesPoolSnapshotsFallback(
                    target.id(),
                    target.manufacturerName(),
                    target.vehicleType(),
                    target.fuelType(),
                    minYear,
                    maxYear
            ).stream().map(VehicleSeriesSnapshot::fromView).toList();
        }
        long poolQueryMs = performanceLogSupport.elapsedMillis(poolQueryStart);

        String targetSeries = extractSeriesName(target);
        String targetModelPrefix = modelPrefix(target.model(), 6);
        List<SameSeriesCandidateDto> candidates = new ArrayList<>();

        long scoreStart = System.nanoTime();
        for (VehicleSeriesSnapshot c : pool) {
            ScoreResult scoreResult = score(target, c, targetSeries, targetModelPrefix);
            if (scoreResult.score < 58) {
                continue;
            }
            candidates.add(new SameSeriesCandidateDto(
                    c.id(),
                    c.brand(),
                    c.model(),
                    c.modelYear(),
                    c.manufacturerName(),
                    c.vehicleType(),
                    c.fuelType(),
                    c.curbWeight(),
                    c.wheelbaseMm(),
                    extractSeriesName(c),
                    scoreResult.score,
                    scoreResult.confidenceLevel,
                    scoreResult.reasons
            ));
        }
        long scoreMs = performanceLogSupport.elapsedMillis(scoreStart);

        long sortStart = System.nanoTime();
        candidates.sort(Comparator
                .comparing(SameSeriesCandidateDto::getScore, Comparator.reverseOrder())
                .thenComparing(SameSeriesCandidateDto::getModelYear, Comparator.reverseOrder())
                .thenComparing(SameSeriesCandidateDto::getVehicleId, Comparator.reverseOrder()));
        if (candidates.size() > limit) {
            candidates = new ArrayList<>(candidates.subList(0, limit));
        }
        long sortMs = performanceLogSupport.elapsedMillis(sortStart);

        int high = 0;
        int medium = 0;
        for (SameSeriesCandidateDto c : candidates) {
            if ("HIGH".equals(c.getConfidenceLevel())) {
                high++;
            } else if ("MEDIUM".equals(c.getConfidenceLevel())) {
                medium++;
            }
        }

        SameSeriesResponse response = new SameSeriesResponse(
                target.id(),
                targetSeries,
                yearWindow,
                candidates.size(),
                high,
                medium,
                candidates
        );

        long totalMs = performanceLogSupport.elapsedMillis(totalStart);
        performanceLogSupport.logStep(
                log,
                "same-series vehicleId=" + vehicleId,
                totalMs,
                "loadTargetMs=" + loadTargetMs
                        + ", poolQueryMs=" + poolQueryMs
                        + ", scoreMs=" + scoreMs
                        + ", sortMs=" + sortMs
                        + ", poolSize=" + pool.size()
                        + ", candidateCount=" + response.getCandidateCount()
                        + ", high=" + response.getHighConfidenceCount()
                        + ", medium=" + response.getMediumConfidenceCount()
                        + ", indexedQuery=" + indexedQuery
        );

        return Optional.of(response);
    }

    private ScoreResult score(VehicleSeriesSnapshot target, VehicleSeriesSnapshot candidate, String targetSeries, String targetModelPrefix) {
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

        String candidatePrefix = modelPrefix(candidate.model(), 6);
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

        if (equalsIgnoreCase(target.manufacturerName(), candidate.manufacturerName())) {
            score += 15;
            reasons.add("生产企业一致");
        }
        if (equalsIgnoreCase(target.vehicleType(), candidate.vehicleType())) {
            score += 8;
            reasons.add("车辆类型一致");
        }
        if (equalsIgnoreCase(target.fuelType(), candidate.fuelType())) {
            score += 8;
            reasons.add("能源类型一致");
        }

        if (target.modelYear() != null && candidate.modelYear() != null) {
            int yearDiff = Math.abs(target.modelYear() - candidate.modelYear());
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

        int weightBonus = closeBonus(target.curbWeight(), candidate.curbWeight(), new BigDecimal("60"), new BigDecimal("120"), 8, 4);
        if (weightBonus > 0) {
            score += weightBonus;
            reasons.add("整备质量接近");
        }

        int wheelbaseBonus = closeBonus(target.wheelbaseMm(), candidate.wheelbaseMm(), 20, 50, 8, 4);
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

    private static String extractSeriesName(VehicleSeriesSnapshot vehicle) {
        String trademark = normalizeSeriesPart(vehicle.trademark());
        if (!trademark.isBlank()) {
            return trademark;
        }
        String brand = normalizeSeriesPart(vehicle.brand());
        if (!brand.isBlank()) {
            return brand;
        }
        String modelPrefix = modelPrefix(vehicle.model(), 6);
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

    private static String normalizeQueryKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoreResult(int score, String confidenceLevel, List<String> reasons) {
    }

    private record VehicleSeriesSnapshot(
            Long id,
            String brand,
            String model,
            Integer modelYear,
            String manufacturerName,
            String vehicleType,
            String fuelType,
            BigDecimal curbWeight,
            Integer wheelbaseMm,
            String trademark,
            String productNo
    ) {
        private static VehicleSeriesSnapshot fromView(VehicleSeriesSnapshotView view) {
            return new VehicleSeriesSnapshot(
                    view.getId(),
                    view.getBrand(),
                    view.getModel(),
                    view.getModelYear(),
                    view.getManufacturerName(),
                    view.getVehicleType(),
                    view.getFuelType(),
                    view.getCurbWeight(),
                    view.getWheelbaseMm(),
                    view.getTrademark(),
                    view.getProductNo()
            );
        }
    }
}
