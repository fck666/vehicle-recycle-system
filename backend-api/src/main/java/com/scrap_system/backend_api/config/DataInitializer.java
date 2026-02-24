package com.scrap_system.backend_api.config;

import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.model.MaterialTemplate;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import com.scrap_system.backend_api.repository.MaterialTemplateRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final VehicleModelRepository vehicleModelRepository;
    private final MaterialTemplateRepository materialTemplateRepository;
    private final MaterialPriceRepository materialPriceRepository;

    @Override
    public void run(String... args) throws Exception {
        if (vehicleModelRepository.count() == 0) {
            initVehicles();
        }
        if (materialTemplateRepository.count() == 0) {
            initTemplates();
        }
        if (materialPriceRepository.count() == 0) {
            initPrices();
        }
    }

    private void initVehicles() {
        createVehicle("Toyota", "Corolla", 2019, "gas", new BigDecimal("1320"), null, "sedan");
        createVehicle("BYD", "Qin EV", 2022, "ev", new BigDecimal("1680"), new BigDecimal("55"), "ev_sedan");
        createVehicle("VW", "Golf", 2018, "gas", new BigDecimal("1340"), null, "hatchback");
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

    private void initTemplates() {
        createTemplate("sedan", new BigDecimal("0.68"), new BigDecimal("0.12"), new BigDecimal("0.03"), new BigDecimal("0.85"));
        createTemplate("ev_sedan", new BigDecimal("0.62"), new BigDecimal("0.14"), new BigDecimal("0.04"), new BigDecimal("0.80"));
        createTemplate("hatchback", new BigDecimal("0.66"), new BigDecimal("0.11"), new BigDecimal("0.03"), new BigDecimal("0.84"));
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

    private void initPrices() {
        createPrice("steel", new BigDecimal("3.10"));
        createPrice("aluminum", new BigDecimal("16.50"));
        createPrice("copper", new BigDecimal("58.00"));
        createPrice("battery", new BigDecimal("4.50"));
    }

    private void createPrice(String type, BigDecimal price) {
        MaterialPrice p = new MaterialPrice();
        p.setType(type);
        p.setPricePerKg(price);
        materialPriceRepository.save(p);
    }
}
