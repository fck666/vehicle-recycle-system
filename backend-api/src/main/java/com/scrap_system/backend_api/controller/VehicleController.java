package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleModelRepository vehicleModelRepository;

    @GetMapping
    public ResponseEntity<List<VehicleModel>> list() {
        return ResponseEntity.ok(vehicleModelRepository.findAll());
    }
}
