package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.VehicleImage;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleImageController {

    private final VehicleModelRepository vehicleModelRepository;
    private final FileStorageService fileStorageService;

    @PostMapping("/{vehicleId}/images")
    public ResponseEntity<?> uploadImage(
            @PathVariable Long vehicleId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "sort", required = false, defaultValue = "0") Integer sort) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("file is empty");
        }

        VehicleModel vehicle = vehicleModelRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        String imageUrl = fileStorageService.uploadFile(file, "vehicles/" + vehicleId);

        VehicleImage image = new VehicleImage();
        image.setVehicle(vehicle);
        image.setImageUrl(imageUrl);
        image.setImageName(name != null ? name : file.getOriginalFilename());
        image.setSortOrder(sort);

        vehicle.getImages().add(image);
        vehicleModelRepository.save(vehicle);

        return ResponseEntity.ok("Image uploaded successfully: " + imageUrl);
    }
}
