package com.scrap_system.backend_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrap_system.backend_api.dto.MaterialValueItem;
import com.scrap_system.backend_api.dto.ValuationResult;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.model.MaterialTemplate;
import com.scrap_system.backend_api.model.MaterialTemplateItem;
import com.scrap_system.backend_api.model.ValuationRecord;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import com.scrap_system.backend_api.repository.MaterialTemplateItemRepository;
import com.scrap_system.backend_api.repository.MaterialTemplateRepository;
import com.scrap_system.backend_api.repository.ValuationRecordRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ValuationService {

    private final VehicleModelRepository vehicleModelRepository;
    private final MaterialTemplateRepository materialTemplateRepository;
    private final MaterialTemplateItemRepository materialTemplateItemRepository;
    private final MaterialPriceRepository materialPriceRepository;
    private final ValuationRecordRepository valuationRecordRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ValuationResult calculateValuation(Long vehicleId) {
        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
        MaterialTemplate template = materialTemplateRepository.findByVehicleType(vehicle.getVehicleType())
                .orElseThrow(() -> new RuntimeException("Material template not found for type: " + vehicle.getVehicleType()));
        Map<String, BigDecimal> ratios = resolveRatios(template);
        return calculateAndSave(vehicleId, vehicle, template.getRecoveryRatio(), ratios);
    }

    @Transactional
    public ValuationResult calculatePreciseValuation(Long vehicleId, com.scrap_system.backend_api.dto.PreciseValuationRequest request) {
        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
        MaterialTemplate template = materialTemplateRepository.findByVehicleType(vehicle.getVehicleType())
                .orElseThrow(() -> new RuntimeException("Material template not found for type: " + vehicle.getVehicleType()));
        Map<String, BigDecimal> ratios = new HashMap<>(resolveRatios(template));
        if (request.getSteelRatio() != null) ratios.put("steel", request.getSteelRatio());
        if (request.getAluminumRatio() != null) ratios.put("aluminum", request.getAluminumRatio());
        if (request.getCopperRatio() != null) ratios.put("copper", request.getCopperRatio());
        VehicleModel working = vehicle;
        if (request.getCurbWeight() != null) {
            working = new VehicleModel();
            working.setId(vehicle.getId());
            working.setCurbWeight(request.getCurbWeight());
        }
        return calculateAndSave(vehicleId, working, template.getRecoveryRatio(), ratios);
    }

    private ValuationResult calculateAndSave(Long vehicleId, VehicleModel vehicle, BigDecimal recoveryRatio, Map<String, BigDecimal> ratios) {
        BigDecimal curbWeight = vehicle.getCurbWeight();
        List<MaterialValueItem> materialValues = ratios.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(e -> {
                    BigDecimal price = getPrice(e.getKey());
                    BigDecimal weight = curbWeight.multiply(e.getValue()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal value = weight.multiply(price).setScale(2, RoundingMode.HALF_UP);
                    return MaterialValueItem.builder()
                            .materialType(e.getKey())
                            .ratio(e.getValue())
                            .weightKg(weight)
                            .pricePerKg(price)
                            .value(value)
                            .build();
                })
                .sorted(Comparator.comparing(MaterialValueItem::getMaterialType))
                .toList();
        BigDecimal sum = materialValues.stream().map(MaterialValueItem::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalValue = sum.multiply(recoveryRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal steelValue = valueOf(materialValues, "steel");
        BigDecimal aluminumValue = valueOf(materialValues, "aluminum");
        BigDecimal copperValue = valueOf(materialValues, "copper");
        BigDecimal batteryValue = valueOf(materialValues, "battery");
        ValuationRecord record = new ValuationRecord();
        record.setVehicleId(vehicleId);
        record.setValuationResult(totalValue);
        record.setSteelValue(steelValue);
        record.setAluminumValue(aluminumValue);
        record.setCopperValue(copperValue);
        record.setBatteryValue(batteryValue);
        record.setDetailsJson(writeDetails(materialValues));
        valuationRecordRepository.save(record);
        return ValuationResult.builder()
                .totalValue(totalValue)
                .steelValue(steelValue)
                .aluminumValue(aluminumValue)
                .copperValue(copperValue)
                .batteryValue(batteryValue)
                .materialValues(materialValues)
                .build();
    }

    private Map<String, BigDecimal> resolveRatios(MaterialTemplate template) {
        List<MaterialTemplateItem> items = materialTemplateItemRepository.findByTemplateIdOrderByIdAsc(template.getId());
        if (!items.isEmpty()) {
            Map<String, BigDecimal> map = new HashMap<>();
            for (MaterialTemplateItem item : items) {
                if (item.getRatio() != null && item.getRatio().compareTo(BigDecimal.ZERO) > 0) {
                    map.put(item.getMaterialType(), item.getRatio());
                }
            }
            return map;
        }
        Map<String, BigDecimal> fallback = new HashMap<>();
        fallback.put("steel", template.getSteelRatio());
        fallback.put("aluminum", template.getAluminumRatio());
        fallback.put("copper", template.getCopperRatio());
        return fallback;
    }

    private static BigDecimal valueOf(List<MaterialValueItem> items, String materialType) {
        return items.stream()
                .filter(i -> materialType.equals(i.getMaterialType()))
                .findFirst()
                .map(MaterialValueItem::getValue)
                .orElse(BigDecimal.ZERO);
    }

    private String writeDetails(List<MaterialValueItem> materialValues) {
        try {
            return objectMapper.writeValueAsString(materialValues);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private BigDecimal getPrice(String type) {
        return materialPriceRepository.findFirstByTypeOrderByEffectiveDateDescFetchedAtDesc(type)
                .map(MaterialPrice::getPricePerKg)
                .orElse(BigDecimal.ZERO);
    }
    
    public List<ValuationRecord> getHistory(Long vehicleId) {
        return valuationRecordRepository.findByVehicleIdOrderByCreatedTimeDesc(vehicleId);
    }
}
