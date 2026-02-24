package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.dto.ValuationResult;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.model.MaterialTemplate;
import com.scrap_system.backend_api.model.ValuationRecord;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import com.scrap_system.backend_api.repository.MaterialTemplateRepository;
import com.scrap_system.backend_api.repository.ValuationRecordRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValuationService {

    private final VehicleModelRepository vehicleModelRepository;
    private final MaterialTemplateRepository materialTemplateRepository;
    private final MaterialPriceRepository materialPriceRepository;
    private final ValuationRecordRepository valuationRecordRepository;

    @Transactional
    public ValuationResult calculateValuation(Long vehicleId) {
        // 1. Get Vehicle
        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));

        // 2. Get Template
        MaterialTemplate template = materialTemplateRepository.findByVehicleType(vehicle.getVehicleType())
                .orElseThrow(() -> new RuntimeException("Material template not found for type: " + vehicle.getVehicleType()));

        // 3. Get Prices
        BigDecimal priceSteel = getPrice("steel");
        BigDecimal priceAluminum = getPrice("aluminum");
        BigDecimal priceCopper = getPrice("copper");
        BigDecimal priceBattery = getPrice("battery");

        // 4. Calculate
        BigDecimal curbWeight = vehicle.getCurbWeight();
        
        BigDecimal steelWeight = curbWeight.multiply(template.getSteelRatio());
        BigDecimal aluminumWeight = curbWeight.multiply(template.getAluminumRatio());
        BigDecimal copperWeight = curbWeight.multiply(template.getCopperRatio());

        BigDecimal steelValue = steelWeight.multiply(priceSteel).setScale(2, RoundingMode.HALF_UP);
        BigDecimal aluminumValue = aluminumWeight.multiply(priceAluminum).setScale(2, RoundingMode.HALF_UP);
        BigDecimal copperValue = copperWeight.multiply(priceCopper).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal batteryValue = BigDecimal.ZERO;
        if (vehicle.getBatteryKwh() != null && vehicle.getBatteryKwh().compareTo(BigDecimal.ZERO) > 0) {
             batteryValue = vehicle.getBatteryKwh().multiply(priceBattery).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalValue = steelValue.add(aluminumValue).add(copperValue).add(batteryValue)
                .multiply(template.getRecoveryRatio()).setScale(2, RoundingMode.HALF_UP);

        // 5. Save Record
        ValuationRecord record = new ValuationRecord();
        record.setVehicleId(vehicleId);
        record.setValuationResult(totalValue);
        record.setSteelValue(steelValue);
        record.setAluminumValue(aluminumValue);
        record.setCopperValue(copperValue);
        record.setBatteryValue(batteryValue);
        valuationRecordRepository.save(record);

        return ValuationResult.builder()
                .totalValue(totalValue)
                .steelValue(steelValue)
                .aluminumValue(aluminumValue)
                .copperValue(copperValue)
                .batteryValue(batteryValue)
                .build();
    }

    @Transactional
    public ValuationResult calculatePreciseValuation(Long vehicleId, com.scrap_system.backend_api.dto.PreciseValuationRequest request) {
        // 1. Get Vehicle
        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));

        // 2. Get Template (for recovery ratio only, ratios are from request)
        MaterialTemplate template = materialTemplateRepository.findByVehicleType(vehicle.getVehicleType())
                .orElseThrow(() -> new RuntimeException("Material template not found for type: " + vehicle.getVehicleType()));

        // 3. Get Prices
        BigDecimal priceSteel = getPrice("steel");
        BigDecimal priceAluminum = getPrice("aluminum");
        BigDecimal priceCopper = getPrice("copper");
        BigDecimal priceBattery = getPrice("battery");

        // 4. Calculate with precise data
        BigDecimal curbWeight = request.getCurbWeight() != null ? request.getCurbWeight() : vehicle.getCurbWeight();
        BigDecimal steelRatio = request.getSteelRatio() != null ? request.getSteelRatio() : template.getSteelRatio();
        BigDecimal aluminumRatio = request.getAluminumRatio() != null ? request.getAluminumRatio() : template.getAluminumRatio();
        BigDecimal copperRatio = request.getCopperRatio() != null ? request.getCopperRatio() : template.getCopperRatio();
        
        BigDecimal steelWeight = curbWeight.multiply(steelRatio);
        BigDecimal aluminumWeight = curbWeight.multiply(aluminumRatio);
        BigDecimal copperWeight = curbWeight.multiply(copperRatio);

        BigDecimal steelValue = steelWeight.multiply(priceSteel).setScale(2, RoundingMode.HALF_UP);
        BigDecimal aluminumValue = aluminumWeight.multiply(priceAluminum).setScale(2, RoundingMode.HALF_UP);
        BigDecimal copperValue = copperWeight.multiply(priceCopper).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal batteryValue = BigDecimal.ZERO;
        BigDecimal batteryKwh = request.getBatteryKwh() != null ? request.getBatteryKwh() : vehicle.getBatteryKwh();
        if (batteryKwh != null && batteryKwh.compareTo(BigDecimal.ZERO) > 0) {
             batteryValue = batteryKwh.multiply(priceBattery).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalValue = steelValue.add(aluminumValue).add(copperValue).add(batteryValue)
                .multiply(template.getRecoveryRatio()).setScale(2, RoundingMode.HALF_UP);

        // 5. Save Record (Maybe mark as precise?)
        ValuationRecord record = new ValuationRecord();
        record.setVehicleId(vehicleId);
        record.setValuationResult(totalValue);
        record.setSteelValue(steelValue);
        record.setAluminumValue(aluminumValue);
        record.setCopperValue(copperValue);
        record.setBatteryValue(batteryValue);
        valuationRecordRepository.save(record);

        return ValuationResult.builder()
                .totalValue(totalValue)
                .steelValue(steelValue)
                .aluminumValue(aluminumValue)
                .copperValue(copperValue)
                .batteryValue(batteryValue)
                .build();
    }

    private BigDecimal getPrice(String type) {
        return materialPriceRepository.findByType(type)
                .map(MaterialPrice::getPricePerKg)
                .orElse(BigDecimal.ZERO);
    }
    
    public List<ValuationRecord> getHistory(Long vehicleId) {
        return valuationRecordRepository.findByVehicleIdOrderByCreatedTimeDesc(vehicleId);
    }
}
