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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValuationService {
    private static final String SCOPE_VEHICLE = "VEHICLE";
    private static final String SCOPE_VEHICLE_TYPE = "VEHICLE_TYPE";
    private static final String OTHERS = "others";
    private static final String PRICING_MODE_WEIGHT = "WEIGHT";
    private static final String PRICING_MODE_FIXED_TOTAL = "FIXED_TOTAL";

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
        MaterialTemplate template = resolveTemplate(vehicle);
        List<TemplateMaterialItem> materialItems = resolveMaterialItems(template);
        return calculateAndSave(vehicleId, vehicle, template.getRecoveryRatio(), materialItems, template.getOthersPricePerKgOverride());
    }

    @Transactional
    public ValuationResult calculatePreciseValuation(Long vehicleId, com.scrap_system.backend_api.dto.PreciseValuationRequest request) {
        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
        MaterialTemplate template = resolveTemplate(vehicle);
        List<TemplateMaterialItem> materialItems = new ArrayList<>(resolveMaterialItems(template));
        overrideWeightRatio(materialItems, "steel", request.getSteelRatio());
        overrideWeightRatio(materialItems, "aluminum", request.getAluminumRatio());
        overrideWeightRatio(materialItems, "copper", request.getCopperRatio());
        VehicleModel working = vehicle;
        if (request.getCurbWeight() != null) {
            working = new VehicleModel();
            working.setId(vehicle.getId());
            working.setCurbWeight(request.getCurbWeight());
        }
        return calculateAndSave(vehicleId, working, template.getRecoveryRatio(), materialItems, template.getOthersPricePerKgOverride());
    }

    private ValuationResult calculateAndSave(Long vehicleId, VehicleModel vehicle, BigDecimal recoveryRatio, List<TemplateMaterialItem> materialItems, BigDecimal othersPriceOverride) {
        BigDecimal curbWeight = vehicle.getCurbWeight();
        if (curbWeight == null || curbWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Vehicle curbWeight invalid for valuation, vehicleId=" + vehicleId);
        }
        if (recoveryRatio == null || recoveryRatio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Template recoveryRatio invalid for valuation, vehicleId=" + vehicleId);
        }
        log.info("Valuation start, vehicleId={}, curbWeight={}, recoveryRatio={}, ratioCount={}",
                vehicleId, curbWeight, recoveryRatio, materialItems == null ? 0 : materialItems.size());
        List<MaterialValueItem> materialValues = materialItems.stream()
                .map(item -> toMaterialValue(item, curbWeight, othersPriceOverride))
                .filter(v -> v != null && v.getValue() != null && v.getValue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(MaterialValueItem::getMaterialType))
                .toList();
        BigDecimal sumWeight = materialValues.stream()
                .filter(v -> !"PART".equals(v.getCategory()))
                .map(MaterialValueItem::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumPart = materialValues.stream()
                .filter(v -> "PART".equals(v.getCategory()))
                .map(MaterialValueItem::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Removed recoveryRatio multiplication as recorded prices are already scrap prices
        BigDecimal totalValue = sumWeight.add(sumPart).setScale(2, RoundingMode.HALF_UP);
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

    private MaterialTemplate resolveTemplate(VehicleModel vehicle) {
        return materialTemplateRepository.findByScopeTypeAndScopeValue(SCOPE_VEHICLE, String.valueOf(vehicle.getId()))
                .or(() -> materialTemplateRepository.findByScopeTypeAndScopeValue(SCOPE_VEHICLE_TYPE, vehicle.getVehicleType()))
                .or(() -> materialTemplateRepository.findByVehicleType(vehicle.getVehicleType()))
                .orElseThrow(() -> new IllegalStateException("Material template not found for vehicle: " + vehicle.getId() + ", type: " + vehicle.getVehicleType()));
    }

    private List<TemplateMaterialItem> resolveMaterialItems(MaterialTemplate template) {
        List<MaterialTemplateItem> items = materialTemplateItemRepository.findByTemplateIdOrderByIdAsc(template.getId());
        if (!items.isEmpty()) {
            List<TemplateMaterialItem> mapped = new ArrayList<>();
            for (MaterialTemplateItem item : items) {
                String mode = normalizePricingMode(item.getPricingMode());
                if (PRICING_MODE_FIXED_TOTAL.equals(mode)) {
                    if (item.getFixedTotalPrice() != null && item.getFixedTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                        mapped.add(new TemplateMaterialItem(item.getMaterialType(), mode, null, item.getFixedTotalPrice()));
                    }
                    continue;
                }
                if (item.getRatio() != null && item.getRatio().compareTo(BigDecimal.ZERO) > 0) {
                    mapped.add(new TemplateMaterialItem(item.getMaterialType(), PRICING_MODE_WEIGHT, item.getRatio(), null));
                }
            }
            return mapped;
        }
        List<TemplateMaterialItem> fallback = new ArrayList<>();
        fallback.add(new TemplateMaterialItem("steel", PRICING_MODE_WEIGHT, template.getSteelRatio(), null));
        fallback.add(new TemplateMaterialItem("aluminum", PRICING_MODE_WEIGHT, template.getAluminumRatio(), null));
        fallback.add(new TemplateMaterialItem("copper", PRICING_MODE_WEIGHT, template.getCopperRatio(), null));
        return fallback;
    }

    private MaterialValueItem toMaterialValue(TemplateMaterialItem item, BigDecimal curbWeight, BigDecimal othersPriceOverride) {
        if (item == null) return null;
        if (PRICING_MODE_FIXED_TOTAL.equals(item.pricingMode)) {
            if (item.fixedTotalPrice == null || item.fixedTotalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return MaterialValueItem.builder()
                    .category("PART")
                    .materialType(item.materialType)
                    .ratio(null)
                    .weightKg(null)
                    .pricePerKg(null)
                    .value(item.fixedTotalPrice.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }
        if (item.ratio == null || item.ratio.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal price = OTHERS.equals(item.materialType) && othersPriceOverride != null ? othersPriceOverride : getPrice(item.materialType);
        BigDecimal weight = curbWeight.multiply(item.ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal value = weight.multiply(price).setScale(2, RoundingMode.HALF_UP);
        return MaterialValueItem.builder()
                .category("MATERIAL")
                .materialType(item.materialType)
                .ratio(item.ratio)
                .weightKg(weight)
                .pricePerKg(price)
                .value(value)
                .build();
    }

    private void overrideWeightRatio(List<TemplateMaterialItem> items, String materialType, BigDecimal ratio) {
        if (ratio == null || items == null) return;
        for (TemplateMaterialItem item : items) {
            if (materialType.equals(item.materialType) && PRICING_MODE_WEIGHT.equals(item.pricingMode)) {
                item.ratio = ratio;
            }
        }
    }

    private String normalizePricingMode(String raw) {
        if (raw == null || raw.trim().isEmpty()) return PRICING_MODE_WEIGHT;
        String mode = raw.trim().toUpperCase(Locale.ROOT);
        if (PRICING_MODE_FIXED_TOTAL.equals(mode)) return PRICING_MODE_FIXED_TOTAL;
        return PRICING_MODE_WEIGHT;
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
        // First try to get RECYCLE price
        return materialPriceRepository.findFirstByTypeAndPriceCategoryOrderByEffectiveDateDesc(type, "RECYCLE")
                .map(MaterialPrice::getPricePerKg)
                // Fallback to MARKET price if no recycle price found (or return ZERO if strict decoupling is desired)
                // Based on user requirement "Valuation template uses recycle price", strict decoupling is safer.
                // However, to avoid sudden 0 value if recycle price is missing, we can fallback or just return 0.
                // The user said "Market price decoupled from valuation system".
                // So we should NOT fallback to market price.
                .orElse(BigDecimal.ZERO);
    }
    
    public List<ValuationRecord> getHistory(Long vehicleId) {
        return valuationRecordRepository.findByVehicleIdOrderByCreatedTimeDesc(vehicleId);
    }

    private static class TemplateMaterialItem {
        private final String materialType;
        private final String pricingMode;
        private BigDecimal ratio;
        private final BigDecimal fixedTotalPrice;

        private TemplateMaterialItem(String materialType, String pricingMode, BigDecimal ratio, BigDecimal fixedTotalPrice) {
            this.materialType = materialType;
            this.pricingMode = pricingMode;
            this.ratio = ratio;
            this.fixedTotalPrice = fixedTotalPrice;
        }
    }
}
