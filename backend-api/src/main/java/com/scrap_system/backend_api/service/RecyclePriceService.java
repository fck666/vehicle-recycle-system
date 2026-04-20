package com.scrap_system.backend_api.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.scrap_system.backend_api.dto.admin.RecyclePriceImportDto;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecyclePriceService {

    private final MaterialPriceRepository materialPriceRepository;
    private static final List<String> DEFAULT_RECYCLE_TYPES = List.of(
            "steel", "aluminum", "copper", "battery", "plastic", "rubber"
    );

    private static final Map<String, String> MATERIAL_NAME_MAP = new HashMap<>();

    static {
        MATERIAL_NAME_MAP.put("废钢", "steel");
        MATERIAL_NAME_MAP.put("铝", "aluminum");
        MATERIAL_NAME_MAP.put("铜", "copper");
        MATERIAL_NAME_MAP.put("电池", "battery");
        MATERIAL_NAME_MAP.put("塑料", "plastic");
        MATERIAL_NAME_MAP.put("橡胶", "rubber");
    }

    public List<MaterialPrice> getRecyclePrices() {
        return materialPriceRepository.findByPriceCategoryOrderByEffectiveDateDesc("RECYCLE");
    }

    public List<String> getRecycleMaterialTypes() {
        List<String> types = materialPriceRepository.findDistinctTypesByCategory("RECYCLE");
        return (types == null || types.isEmpty()) ? DEFAULT_RECYCLE_TYPES : types;
    }

    @Transactional
    public void importRecyclePrices(MultipartFile file) throws IOException {
        EasyExcel.read(file.getInputStream(), RecyclePriceImportDto.class, new ReadListener<RecyclePriceImportDto>() {
            private static final int BATCH_COUNT = 100;
            private List<RecyclePriceImportDto> cachedDataList = new ArrayList<>(BATCH_COUNT);

            @Override
            public void invoke(RecyclePriceImportDto data, AnalysisContext context) {
                cachedDataList.add(data);
                if (cachedDataList.size() >= BATCH_COUNT) {
                    saveData();
                    cachedDataList.clear();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                saveData();
            }

            private void saveData() {
                for (RecyclePriceImportDto dto : cachedDataList) {
                    processAndSave(dto, "EXCEL_IMPORT", false);
                }
            }
        }).sheet().doRead();
    }

    @Transactional
    public void saveRecyclePrice(RecyclePriceImportDto dto) {
        processAndSave(dto, "MANUAL_ENTRY", true);
    }

    @Transactional
    public void deleteRecyclePriceItem(Long id) {
        MaterialPrice existing = materialPriceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if (!"RECYCLE".equals(existing.getPriceCategory())) {
            throw new IllegalArgumentException("仅支持删除回收价格记录");
        }
        materialPriceRepository.deleteById(id);
    }

    @Transactional
    public void deleteRecyclePriceType(String type) {
        // Delete all recycle prices for the given type
        materialPriceRepository.deleteByTypeAndPriceCategory(type, "RECYCLE");
    }

    private void processAndSave(RecyclePriceImportDto dto, String sourceName, boolean strict) {
        if (dto.getMaterialName() == null || dto.getPrice() == null) {
            if (strict) {
                throw new IllegalArgumentException("材料类型和价格不能为空");
            }
            return;
        }

        String materialName = dto.getMaterialName().trim();
        // Allow arbitrary material names, remove restriction to MATERIAL_NAME_MAP
        String type = MATERIAL_NAME_MAP.get(materialName);
        if (type == null) {
            type = materialName; // If not in predefined map, use the name directly
        }

        // Calculate price per kg.
        // Input logic:
        // "吨" (ton) -> input is price/ton -> per kg = price / 1000
        // "斤" (jin) -> input is price/jin -> per kg = price * 2
        // "公斤", "kg", "千克" -> input is price/kg -> per kg = price
        BigDecimal pricePerKg;
        String unit = dto.getUnit() != null ? dto.getUnit().trim() : "千克"; // Default unit is now "千克"
        BigDecimal inputPrice = BigDecimal.valueOf(dto.getPrice());

        if ("吨".equals(unit)) {
            pricePerKg = inputPrice.divide(BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP);
        } else if ("斤".equals(unit)) {
            pricePerKg = inputPrice.multiply(BigDecimal.valueOf(2));
        } else if ("公斤".equals(unit) || "kg".equalsIgnoreCase(unit) || "千克".equals(unit)) {
             pricePerKg = inputPrice;
        } else {
             // Fallback: treat unknown unit as "千克" (price/kg) or maybe log warning. 
             // Given requirement "default unit is kg", we treat as kg.
             pricePerKg = inputPrice;
        }

        LocalDate effectiveDate = dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDate.now();

        MaterialPrice existing;
        if (dto.getId() != null) {
            existing = materialPriceRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
            if (!"RECYCLE".equals(existing.getPriceCategory())) {
                throw new IllegalArgumentException("仅支持编辑回收价格记录");
            }
        } else {
            existing = materialPriceRepository.findByTypeAndEffectiveDateAndPriceCategory(type, effectiveDate, "RECYCLE")
                    .orElse(new MaterialPrice());
        }

        existing.setType(type);
        existing.setPricePerKg(pricePerKg);
        existing.setUnit("kg"); // Stored unit is always kg
        existing.setCurrency("CNY");
        existing.setEffectiveDate(dto.getId() != null && dto.getEffectiveDate() == null
                ? existing.getEffectiveDate()
                : effectiveDate);
        existing.setFetchedAt(java.time.LocalDateTime.now());
        existing.setSourceName(sourceName);
        existing.setPriceCategory("RECYCLE");

        materialPriceRepository.save(existing);
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return LocalDate.now();
        try {
            // EasyExcel might return date as "yyyy-MM-dd HH:mm:ss" or just "yyyy-MM-dd"
            if (dateStr.contains(" ")) {
                return LocalDate.parse(dateStr.split(" ")[0]);
            }
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}, using today", dateStr);
            return LocalDate.now();
        }
    }
}
