package com.scrap_system.backend_api;

import com.scrap_system.backend_api.dto.ValuationResult;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.model.MaterialTemplate;
import com.scrap_system.backend_api.model.ValuationRecord;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import com.scrap_system.backend_api.repository.MaterialTemplateRepository;
import com.scrap_system.backend_api.repository.ValuationRecordRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.service.ValuationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ValuationServiceTest {

    @Autowired
    private ValuationService valuationService;

    @Autowired
    private VehicleModelRepository vehicleModelRepository;

    @Autowired
    private MaterialTemplateRepository materialTemplateRepository;

    @Autowired
    private MaterialPriceRepository materialPriceRepository;

    @Autowired
    private ValuationRecordRepository valuationRecordRepository;

    @Test
    void testCalculateValuation() {
        // Setup data
        createVehicle("Toyota", "Corolla", 2019, "gas", new BigDecimal("1320"), null, "sedan");
        createTemplate("sedan", new BigDecimal("0.68"), new BigDecimal("0.12"), new BigDecimal("0.03"), new BigDecimal("0.85"));
        createPrice("steel", new BigDecimal("3.10"));
        createPrice("aluminum", new BigDecimal("16.50"));
        createPrice("copper", new BigDecimal("58.00"));
        createPrice("battery", new BigDecimal("4.50"));

        // 1. Find vehicle
        VehicleModel vehicle = vehicleModelRepository.findAll().stream()
                .filter(v -> "Toyota".equals(v.getBrand()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        Long vehicleId = vehicle.getId();

        // 2. Calculate
        ValuationResult result = valuationService.calculateValuation(vehicleId);

        // 3. Assert
        assertNotNull(result);
        assertTrue(result.getTotalValue().compareTo(BigDecimal.ZERO) > 0);
        
        // Check calculation logic roughly
        // Toyota Corolla: 1320kg, sedan template
        // Steel: 1320 * 0.68 * 3.10 = 2782.56
        // Aluminum: 1320 * 0.12 * 16.50 = 2613.60
        // Copper: 1320 * 0.03 * 58.00 = 2296.80
        // Total before recovery: 7692.96
        // Recovery: 0.85 -> 6539.016
        
        System.out.println("Calculated Total: " + result.getTotalValue());
        
        // 4. Check Record
        List<ValuationRecord> history = valuationService.getHistory(vehicleId);
        assertFalse(history.isEmpty());
        assertEquals(result.getTotalValue(), history.get(0).getValuationResult());
    }

    private void createVehicle(String brand, String model, Integer year, String fuelType, BigDecimal curbWeight, BigDecimal batteryKwh, String vehicleType) {
        VehicleModel v = new VehicleModel();
        v.setBrand(brand);
        v.setModel(model);
        v.setModelYear(year);
        v.setFuelType(fuelType);
        v.setCurbWeight(curbWeight);
        v.setBatteryKwh(batteryKwh);
        v.setVehicleType(vehicleType);
        vehicleModelRepository.save(v);
    }

    private void createTemplate(String vehicleType, BigDecimal steel, BigDecimal aluminum, BigDecimal copper, BigDecimal recovery) {
        MaterialTemplate t = new MaterialTemplate();
        t.setVehicleType(vehicleType);
        t.setSteelRatio(steel);
        t.setAluminumRatio(aluminum);
        t.setCopperRatio(copper);
        t.setRecoveryRatio(recovery);
        materialTemplateRepository.save(t);
    }

    private void createPrice(String type, BigDecimal price) {
        MaterialPrice p = new MaterialPrice();
        p.setType(type);
        p.setPricePerKg(price);
        materialPriceRepository.save(p);
    }
}
