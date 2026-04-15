package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.model.enums.VehicleSourceType;
import com.scrap_system.backend_api.repository.VehicleDocumentRepository;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVehicleControllerTest {
    private VehicleModelRepository vehicleModelRepository;
    private VehicleDocumentRepository vehicleDocumentRepository;
    private AdminVehicleController controller;

    @BeforeEach
    void setUp() {
        vehicleModelRepository = mock(VehicleModelRepository.class);
        vehicleDocumentRepository = mock(VehicleDocumentRepository.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        controller = new AdminVehicleController(vehicleModelRepository, vehicleDocumentRepository, fileStorageService);
    }

    @Test
    void searchUsesIdentifierFastPathWhenItFindsRows() {
        VehicleModel vehicle = vehicle(21169L, "CBK402E2301");
        Page<VehicleModel> fastPage = new PageImpl<>(List.of(vehicle), PageRequest.of(0, 20), 1);
        when(vehicleModelRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(fastPage);

        ResponseEntity<Page<VehicleModel>> response = controller.search(
                "CBK402E", null, null, null, null, null, null, null, 0, 20, "id,desc"
        );

        Page<VehicleModel> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getTotalElements());
        assertEquals("CBK402E2301", body.getContent().get(0).getProductNo());
        verify(vehicleModelRepository, times(1)).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void searchFallsBackToFuzzyWhenIdentifierFastPathIsEmpty() {
        Page<VehicleModel> emptyFastPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        VehicleModel fallbackVehicle = vehicle(1L, "XXCBK402E");
        Page<VehicleModel> fuzzyPage = new PageImpl<>(List.of(fallbackVehicle), PageRequest.of(0, 20), 1);
        when(vehicleModelRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(emptyFastPage, fuzzyPage);

        ResponseEntity<Page<VehicleModel>> response = controller.search(
                "CBK402E", null, null, null, null, null, null, null, 0, 20, "id,desc"
        );

        Page<VehicleModel> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getTotalElements());
        assertEquals("XXCBK402E", body.getContent().get(0).getProductNo());
        verify(vehicleModelRepository, times(2)).findAll(anySpecification(), any(Pageable.class));
    }

    private static VehicleModel vehicle(Long id, String productNo) {
        VehicleModel vehicle = new VehicleModel();
        vehicle.setId(id);
        vehicle.setSourceType(VehicleSourceType.CRAWLED);
        vehicle.setBrand("丰田(TOYOTA)牌");
        vehicle.setModel("GTM70007RNBEV");
        vehicle.setModelYear(2023);
        vehicle.setFuelType("EV");
        vehicle.setVehicleType("sedan");
        vehicle.setCurbWeight(new BigDecimal("1500.00"));
        vehicle.setProductNo(productNo);
        return vehicle;
    }

    @SuppressWarnings("unchecked")
    private static Specification<VehicleModel> anySpecification() {
        return any(Specification.class);
    }
}
