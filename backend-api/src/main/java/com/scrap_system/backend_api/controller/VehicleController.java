package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleModelRepository vehicleModelRepository;

    @GetMapping
    public ResponseEntity<Page<VehicleModel>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) List<String> manufacturers,
            @RequestParam(required = false) List<String> vehicleTypes,
            @RequestParam(required = false) List<String> fuelTypes,
            @RequestParam(required = false) Integer batchNoMin,
            @RequestParam(required = false) Integer batchNoMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        Sort sortObj = Sort.by(Sort.Direction.DESC, "id");
        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            if (parts.length > 0) {
                String property = parts[0].trim();
                Sort.Direction direction = Sort.Direction.ASC;
                if (parts.length > 1) {
                    if ("desc".equalsIgnoreCase(parts[1].trim())) {
                        direction = Sort.Direction.DESC;
                    }
                }
                sortObj = Sort.by(direction, property);
            }
        }
        
        PageRequest pageable = PageRequest.of(safePage, safeSize, sortObj);

        Specification<VehicleModel> spec = com.scrap_system.backend_api.specification.VehicleSpecs.withDynamicQuery(
                q, brands, manufacturers, vehicleTypes, fuelTypes, null, batchNoMin, batchNoMax
        );

        return ResponseEntity.ok(vehicleModelRepository.findAll(spec, pageable));
    }

    @GetMapping("/facets")
    public ResponseEntity<Map<String, List<String>>> facets() {
        return ResponseEntity.ok(Map.of(
                "brands", vehicleModelRepository.findDistinctBrands(),
                "manufacturers", vehicleModelRepository.findDistinctManufacturers(),
                "vehicleTypes", vehicleModelRepository.findDistinctVehicleTypes(),
                "fuelTypes", vehicleModelRepository.findDistinctFuelTypes()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleModel> get(@PathVariable Long id) {
        return vehicleModelRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
