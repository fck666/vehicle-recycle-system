package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.MaterialTemplateUpsertRequest;
import com.scrap_system.backend_api.dto.MaterialRatioItem;
import com.scrap_system.backend_api.dto.MaterialTemplateDto;
import com.scrap_system.backend_api.model.MaterialTemplate;
import com.scrap_system.backend_api.model.MaterialTemplateItem;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.MaterialTemplateItemRepository;
import com.scrap_system.backend_api.repository.MaterialTemplateRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/material-templates")
@RequiredArgsConstructor
public class MaterialTemplateController {
    private static final String OTHERS = "others";
    private static final String SCOPE_VEHICLE_TYPE = "VEHICLE_TYPE";
    private static final String SCOPE_VEHICLE = "VEHICLE";
    private static final String PRICING_MODE_WEIGHT = "WEIGHT";
    private static final String PRICING_MODE_FIXED_TOTAL = "FIXED_TOTAL";

    private final MaterialTemplateRepository materialTemplateRepository;
    private final MaterialTemplateItemRepository materialTemplateItemRepository;
    private final VehicleModelRepository vehicleModelRepository;

    @GetMapping
    public List<MaterialTemplateDto> list() {
        List<MaterialTemplate> templates = materialTemplateRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(MaterialTemplate::getScopeType, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(MaterialTemplate::getScopeValue, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        Map<Long, List<MaterialTemplateItem>> grouped = loadItems(templates);
        return templates.stream().map(t -> toDto(t, grouped.getOrDefault(t.getId(), List.of()))).toList();
    }

    @GetMapping("/{vehicleType}")
    public ResponseEntity<MaterialTemplateDto> getByVehicleType(@PathVariable String vehicleType) {
        return materialTemplateRepository.findByScopeTypeAndScopeValue(SCOPE_VEHICLE_TYPE, vehicleType.trim())
                .map(t -> {
                    List<MaterialTemplateItem> items = materialTemplateItemRepository.findByTemplateIdOrderByIdAsc(t.getId());
                    return ResponseEntity.ok(toDto(t, items));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/vehicle/{vehicleId}/materials")
    public ResponseEntity<List<MaterialRatioItem>> getMaterialsByVehicle(@PathVariable Long vehicleId) {
        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }
        MaterialTemplate template = materialTemplateRepository.findByScopeTypeAndScopeValue(SCOPE_VEHICLE, String.valueOf(vehicleId))
                .or(() -> materialTemplateRepository.findByScopeTypeAndScopeValue(SCOPE_VEHICLE_TYPE, vehicle.getVehicleType()))
                .or(() -> materialTemplateRepository.findByVehicleType(vehicle.getVehicleType()))
                .orElse(null);
        if (template == null) {
            return ResponseEntity.ok(List.of());
        }
        List<MaterialTemplateItem> items = materialTemplateItemRepository.findByTemplateIdOrderByIdAsc(template.getId());
        return ResponseEntity.ok(items.stream().sorted(Comparator.comparing(MaterialTemplateItem::getMaterialType)).map(i -> {
            MaterialRatioItem r = new MaterialRatioItem();
            r.setMaterialType(i.getMaterialType());
            r.setRatio(i.getRatio());
            r.setPricingMode(normalizePricingMode(i.getPricingMode()));
            r.setFixedTotalPrice(i.getFixedTotalPrice());
            return r;
        }).toList());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<MaterialTemplateDto> upsert(@RequestBody MaterialTemplateUpsertRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getRecoveryRatio() == null || request.getMaterials() == null || request.getMaterials().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<MaterialRatioItem> normalizedItems = applyOthersRule(normalize(request.getMaterials()));
        if (normalizedItems.isEmpty()) return ResponseEntity.badRequest().build();
        BigDecimal sum = normalizedItems.stream()
                .filter(i -> PRICING_MODE_WEIGHT.equals(normalizePricingMode(i.getPricingMode())))
                .map(MaterialRatioItem::getRatio)
                .filter(r -> r != null && r.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ONE) > 0) {
            return ResponseEntity.badRequest().build();
        }
        String scopeType = normalizeScopeType(request.getScopeType());
        String scopeValue = normalizeScopeValue(scopeType, request);
        if (isBlank(scopeValue)) return ResponseEntity.badRequest().build();
        Optional<MaterialTemplate> existing = materialTemplateRepository.findByScopeTypeAndScopeValue(scopeType, scopeValue);
        MaterialTemplate t = existing.orElseGet(MaterialTemplate::new);
        t.setScopeType(scopeType);
        t.setScopeValue(scopeValue);
        t.setVehicleType(SCOPE_VEHICLE_TYPE.equals(scopeType) ? scopeValue : null);
        t.setSteelRatio(findRatio(normalizedItems, "steel"));
        t.setAluminumRatio(findRatio(normalizedItems, "aluminum"));
        t.setCopperRatio(findRatio(normalizedItems, "copper"));
        t.setRecoveryRatio(request.getRecoveryRatio());
        t.setOthersPricePerKgOverride(request.getOthersPricePerKgOverride());

        MaterialTemplate saved = materialTemplateRepository.save(t);
        materialTemplateItemRepository.deleteByTemplateId(saved.getId());
        List<MaterialTemplateItem> toSave = new ArrayList<>();
        for (MaterialRatioItem item : normalizedItems) {
            MaterialTemplateItem tItem = new MaterialTemplateItem();
            tItem.setTemplateId(saved.getId());
            tItem.setMaterialType(item.getMaterialType());
            tItem.setRatio(item.getRatio());
            tItem.setPricingMode(normalizePricingMode(item.getPricingMode()));
            tItem.setFixedTotalPrice(item.getFixedTotalPrice());
            toSave.add(tItem);
        }
        materialTemplateItemRepository.saveAll(toSave);
        return ResponseEntity.ok(toDto(saved, materialTemplateItemRepository.findByTemplateIdOrderByIdAsc(saved.getId())));
    }

    @DeleteMapping("/{vehicleType}")
    @Transactional
    public ResponseEntity<Void> deleteByVehicleType(@PathVariable String vehicleType) {
        Optional<MaterialTemplate> existing = materialTemplateRepository.findByScopeTypeAndScopeValue(SCOPE_VEHICLE_TYPE, vehicleType.trim());
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        materialTemplateRepository.delete(existing.get());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/id/{id}")
    @Transactional
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        Optional<MaterialTemplate> existing = materialTemplateRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        materialTemplateRepository.delete(existing.get());
        return ResponseEntity.noContent().build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static List<MaterialRatioItem> normalize(List<MaterialRatioItem> materials) {
        Map<String, BigDecimal> merged = new HashMap<>();
        Map<String, String> pricingModes = new HashMap<>();
        Map<String, BigDecimal> fixedTotals = new HashMap<>();
        for (MaterialRatioItem item : materials) {
            if (item == null || isBlank(item.getMaterialType())) {
                continue;
            }
            String type = normalizeType(item.getMaterialType());
            String pricingMode = normalizePricingMode(item.getPricingMode());
            if (PRICING_MODE_FIXED_TOTAL.equals(pricingMode)) {
                if (item.getFixedTotalPrice() == null || item.getFixedTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                fixedTotals.put(type, item.getFixedTotalPrice().setScale(2, RoundingMode.HALF_UP));
                merged.put(type, null);
                pricingModes.put(type, PRICING_MODE_FIXED_TOTAL);
            } else {
                if (item.getRatio() == null || item.getRatio().compareTo(BigDecimal.ZERO) < 0) {
                    continue;
                }
                merged.put(type, item.getRatio());
                pricingModes.put(type, PRICING_MODE_WEIGHT);
                fixedTotals.remove(type);
            }
        }
        return merged.entrySet().stream()
                .map(e -> {
                    MaterialRatioItem i = new MaterialRatioItem();
                    i.setMaterialType(e.getKey());
                    String pricingMode = pricingModes.getOrDefault(e.getKey(), PRICING_MODE_WEIGHT);
                    i.setPricingMode(pricingMode);
                    if (PRICING_MODE_FIXED_TOTAL.equals(pricingMode)) {
                        i.setRatio(null);
                        i.setFixedTotalPrice(fixedTotals.get(e.getKey()));
                    } else {
                        i.setRatio(e.getValue() == null ? null : e.getValue().setScale(4, RoundingMode.HALF_UP));
                        i.setFixedTotalPrice(null);
                    }
                    return i;
                })
                .sorted(Comparator.comparing(MaterialRatioItem::getMaterialType))
                .toList();
    }

    private static List<MaterialRatioItem> applyOthersRule(List<MaterialRatioItem> items) {
        if (items.isEmpty()) return items;
        BigDecimal nonOthersSum = items.stream()
                .filter(i -> PRICING_MODE_WEIGHT.equals(normalizePricingMode(i.getPricingMode())))
                .filter(i -> !OTHERS.equals(i.getMaterialType()))
                .map(MaterialRatioItem::getRatio)
                .filter(r -> r != null && r.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (nonOthersSum.compareTo(BigDecimal.ONE) > 0) return List.of();
        boolean hasOthers = items.stream().anyMatch(i ->
                OTHERS.equals(i.getMaterialType()) && PRICING_MODE_WEIGHT.equals(normalizePricingMode(i.getPricingMode())));
        if (!hasOthers) return items;
        BigDecimal othersRatio = BigDecimal.ONE.subtract(nonOthersSum).setScale(4, RoundingMode.HALF_UP);
        List<MaterialRatioItem> adjusted = new ArrayList<>();
        adjusted.addAll(items.stream()
                .filter(i -> !OTHERS.equals(i.getMaterialType()) || !PRICING_MODE_WEIGHT.equals(normalizePricingMode(i.getPricingMode())))
                .toList());
        if (othersRatio.compareTo(BigDecimal.ZERO) > 0) {
            MaterialRatioItem o = new MaterialRatioItem();
            o.setMaterialType(OTHERS);
            o.setRatio(othersRatio);
            o.setPricingMode(PRICING_MODE_WEIGHT);
            adjusted.add(o);
        }
        return adjusted.stream().sorted(Comparator.comparing(MaterialRatioItem::getMaterialType)).toList();
    }

    private static String normalizeType(String rawType) {
        String t = rawType.trim().toLowerCase(Locale.ROOT);
        if ("other".equals(t) || "others".equals(t) || "其它".equals(t) || "其他".equals(t) || "其余".equals(t)) {
            return OTHERS;
        }
        return t;
    }

    private static String normalizeScopeType(String raw) {
        if (isBlank(raw)) return SCOPE_VEHICLE_TYPE;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (SCOPE_VEHICLE.equals(s)) return SCOPE_VEHICLE;
        return SCOPE_VEHICLE_TYPE;
    }

    private static String normalizePricingMode(String raw) {
        if (isBlank(raw)) return PRICING_MODE_WEIGHT;
        String mode = raw.trim().toUpperCase(Locale.ROOT);
        if (PRICING_MODE_FIXED_TOTAL.equals(mode)) return PRICING_MODE_FIXED_TOTAL;
        return PRICING_MODE_WEIGHT;
    }

    private static String normalizeScopeValue(String scopeType, MaterialTemplateUpsertRequest request) {
        String raw = request.getScopeValue();
        if (isBlank(raw)) raw = request.getVehicleType();
        if (isBlank(raw)) return null;
        String value = raw.trim();
        if (SCOPE_VEHICLE.equals(scopeType)) return value;
        return value;
    }

    private static BigDecimal findRatio(List<MaterialRatioItem> items, String type) {
        return items.stream()
                .filter(i -> type.equals(i.getMaterialType()) && PRICING_MODE_WEIGHT.equals(normalizePricingMode(i.getPricingMode())))
                .findFirst()
                .map(MaterialRatioItem::getRatio)
                .orElse(BigDecimal.ZERO);
    }

    private Map<Long, List<MaterialTemplateItem>> loadItems(List<MaterialTemplate> templates) {
        if (templates.isEmpty()) return Map.of();
        List<Long> ids = templates.stream().map(MaterialTemplate::getId).toList();
        return materialTemplateItemRepository.findByTemplateIdIn(ids).stream()
                .collect(Collectors.groupingBy(MaterialTemplateItem::getTemplateId));
    }

    private static MaterialTemplateDto toDto(MaterialTemplate t, List<MaterialTemplateItem> items) {
        List<MaterialRatioItem> materials = items.stream()
                .sorted(Comparator.comparing(MaterialTemplateItem::getMaterialType))
                .map(i -> {
                    MaterialRatioItem r = new MaterialRatioItem();
                    r.setMaterialType(i.getMaterialType());
                    r.setRatio(i.getRatio());
                    r.setPricingMode(normalizePricingMode(i.getPricingMode()));
                    r.setFixedTotalPrice(i.getFixedTotalPrice());
                    return r;
                })
                .toList();
        return MaterialTemplateDto.builder()
                .id(t.getId())
                .vehicleType(t.getVehicleType())
                .scopeType(t.getScopeType())
                .scopeValue(t.getScopeValue())
                .recoveryRatio(t.getRecoveryRatio())
                .othersPricePerKgOverride(t.getOthersPricePerKgOverride())
                .createdAt(t.getCreatedAt())
                .materials(materials)
                .build();
    }
}
