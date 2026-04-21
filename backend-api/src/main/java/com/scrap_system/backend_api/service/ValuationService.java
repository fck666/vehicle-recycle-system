package com.scrap_system.backend_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrap_system.backend_api.dto.SameSeriesCandidateDto;
import com.scrap_system.backend_api.dto.ValuationDimension;
import com.scrap_system.backend_api.dto.ValuationResult;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.model.ValuationRecord;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import com.scrap_system.backend_api.repository.ValuationRecordRepository;
import com.scrap_system.backend_api.repository.VehicleDismantleRecordRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.repository.projection.VehicleSeriesSnapshotView;
import com.scrap_system.backend_api.repository.projection.VehicleValuationSourceView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValuationService {

    private final VehicleModelRepository vehicleModelRepository;
    private final MaterialPriceRepository materialPriceRepository;
    private final ValuationRecordRepository valuationRecordRepository;
    private final VehicleDismantleRecordRepository dismantleRecordRepository;
    private final SameSeriesService sameSeriesService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ValuationResult calculateValuation(Long vehicleId) {
        VehicleSeriesSnapshotView vehicle = vehicleModelRepository.findSeriesSnapshotById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));

        // Pre-fetch all prices into a map to avoid N+1 queries during loop
        Map<String, BigDecimal> priceMap = new HashMap<>();
        List<MaterialPrice> allRecyclePrices = materialPriceRepository.findByPriceCategoryOrderByEffectiveDateDesc("RECYCLE");
        for (MaterialPrice mp : allRecyclePrices) {
            priceMap.putIfAbsent(mp.getType(), mp.getPricePerKg()); // putIfAbsent ensures we only keep the latest (first) since it's ordered by desc
        }

        // 1. Get exact match records (Same productNo or just the same vehicle ID if no productNo)
        List<Long> exactMatchVehicleIds = new ArrayList<>();
        if (vehicle.getProductNo() != null && !vehicle.getProductNo().trim().isEmpty()) {
            exactMatchVehicleIds = vehicleModelRepository.findIdsByProductNoOrderByIdDesc(vehicle.getProductNo());
        } else {
            exactMatchVehicleIds.add(vehicle.getId());
        }
        List<VehicleValuationSourceView> exactMatchRecords = loadValuationSources(exactMatchVehicleIds);

        // 2. Get same series match records using SameSeriesService
        Set<Long> highSeriesVehicleIds = new LinkedHashSet<>();
        Set<Long> mediumSeriesVehicleIds = new LinkedHashSet<>();
        
        sameSeriesService.findSameSeries(vehicle.getId(), 4, 50).ifPresent(response -> {
            for (SameSeriesCandidateDto candidate : response.getCandidates()) {
                if ("HIGH".equals(candidate.getConfidenceLevel())) {
                    highSeriesVehicleIds.add(candidate.getVehicleId());
                } else if ("MEDIUM".equals(candidate.getConfidenceLevel())) {
                    mediumSeriesVehicleIds.add(candidate.getVehicleId());
                }
            }
        });

        // Always include current vehicle in high confidence pool if not already there
        highSeriesVehicleIds.add(vehicle.getId());

        List<VehicleValuationSourceView> seriesHighRecords = loadValuationSources(new ArrayList<>(highSeriesVehicleIds));
        List<VehicleValuationSourceView> seriesMediumRecords = loadValuationSources(new ArrayList<>(mediumSeriesVehicleIds));

        // 3. Compute dimensions
        ValuationDimension exactDimension = computeDimension(exactMatchRecords, priceMap);
        ValuationDimension seriesHighDimension = computeDimension(seriesHighRecords, priceMap);
        ValuationDimension seriesMediumDimension = computeDimension(seriesMediumRecords, priceMap);

        BigDecimal totalValue = exactDimension.getRecordCount() > 0 ? exactDimension.getAvgValue() : seriesHighDimension.getAvgValue();
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) == 0) {
            totalValue = seriesMediumDimension.getAvgValue();
        }
        if (totalValue == null) totalValue = BigDecimal.ZERO;

        // 4. Save history record (optional, keeping for compatibility if needed)
        ValuationRecord record = new ValuationRecord();
        record.setVehicleId(vehicleId);
        record.setValuationResult(totalValue);
        record.setSteelValue(BigDecimal.ZERO);
        record.setAluminumValue(BigDecimal.ZERO);
        record.setCopperValue(BigDecimal.ZERO);
        record.setBatteryValue(BigDecimal.ZERO);
        record.setDetailsJson("{\"source\":\"DATA_DRIVEN\"}");
        valuationRecordRepository.save(record);

        return ValuationResult.builder()
                .totalValue(totalValue)
                .exactMatch(exactDimension)
                .seriesHighMatch(seriesHighDimension)
                .seriesMediumMatch(seriesMediumDimension)
                .build();
    }

    private List<VehicleValuationSourceView> loadValuationSources(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return List.of();
        }
        return dismantleRecordRepository.findValuationSourcesByVehicleIdIn(vehicleIds);
    }

    private ValuationDimension computeDimension(List<VehicleValuationSourceView> records, Map<String, BigDecimal> priceMap) {
        if (records == null || records.isEmpty()) {
            return emptyDimension();
        }

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = null;
        int count = 0;

        for (VehicleValuationSourceView record : records) {
            BigDecimal recordValue = calculateRecordValue(record, priceMap);
            if (recordValue == null || recordValue.compareTo(BigDecimal.ZERO) <= 0) continue;

            sum = sum.add(recordValue);
            if (min == null || recordValue.compareTo(min) < 0) min = recordValue;
            if (max == null || recordValue.compareTo(max) > 0) max = recordValue;
            count++;
        }

        if (count == 0) {
            return emptyDimension();
        }

        BigDecimal avg = sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
        return ValuationDimension.builder()
                .recordCount(count)
                .avgValue(avg)
                .minValue(min)
                .maxValue(max)
                .build();
    }

    private ValuationDimension emptyDimension() {
        return ValuationDimension.builder()
                .recordCount(0)
                .avgValue(BigDecimal.ZERO)
                .minValue(BigDecimal.ZERO)
                .maxValue(BigDecimal.ZERO)
                .build();
    }

    private BigDecimal calculateRecordValue(VehicleValuationSourceView record, Map<String, BigDecimal> priceMap) {
        BigDecimal value = BigDecimal.ZERO;
        
        value = value.add(safeMultiply(record.getSteelWeight(), priceMap.getOrDefault("steel", BigDecimal.ZERO)));
        value = value.add(safeMultiply(record.getAluminumWeight(), priceMap.getOrDefault("aluminum", BigDecimal.ZERO)));
        value = value.add(safeMultiply(record.getCopperWeight(), priceMap.getOrDefault("copper", BigDecimal.ZERO)));
        value = value.add(safeMultiply(record.getBatteryWeight(), priceMap.getOrDefault("battery", BigDecimal.ZERO)));
        value = value.add(safeMultiply(record.getOtherWeight(), priceMap.getOrDefault("others", BigDecimal.ZERO)));

        if (record.getDetailsJson() != null && !record.getDetailsJson().trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(record.getDetailsJson());
                JsonNode items = root.get("items");
                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        String category = item.path("category").asText("");
                        if ("PART".equals(category)) {
                            boolean isPremium = item.path("isPremium").asBoolean(false);
                            if (!isPremium) {
                                BigDecimal totalPrice = new BigDecimal(item.path("totalPrice").asText("0"));
                                value = value.add(totalPrice);
                            }
                        } else if ("MATERIAL".equals(category)) {
                            String matType = item.path("materialType").asText("");
                            BigDecimal weight = new BigDecimal(item.path("weightKg").asText("0"));
                            value = value.add(safeMultiply(weight, priceMap.getOrDefault(matType, BigDecimal.ZERO)));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse detailsJson for record {}", record.getId(), e);
            }
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeMultiply(BigDecimal weight, BigDecimal price) {
        if (weight == null || price == null) return BigDecimal.ZERO;
        return weight.multiply(price);
    }
    
    public List<ValuationRecord> getHistory(Long vehicleId) {
        return valuationRecordRepository.findByVehicleIdOrderByCreatedTimeDesc(vehicleId);
    }
}
