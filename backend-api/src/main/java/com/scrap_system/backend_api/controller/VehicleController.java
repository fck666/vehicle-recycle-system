package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.config.PerformanceLogSupport;
import com.scrap_system.backend_api.dto.SameSeriesResponse;
import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.VehicleModelRepository;
import com.scrap_system.backend_api.service.FileStorageService;
import com.scrap_system.backend_api.service.SameSeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class VehicleController {

    private final VehicleModelRepository vehicleModelRepository;
    private final FileStorageService fileStorageService;
    private final SameSeriesService sameSeriesService;
    private final PerformanceLogSupport performanceLogSupport;

    @GetMapping
    public ResponseEntity<Page<VehicleModel>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) List<String> manufacturers,
            @RequestParam(required = false) List<String> vehicleTypes,
            @RequestParam(required = false) List<String> fuelTypes,
            @RequestParam(required = false) Integer batchNoMin,
            @RequestParam(required = false) Integer batchNoMax,
            @RequestParam(required = false) Boolean hasDismantleRecord,
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
                q, brands, manufacturers, vehicleTypes, fuelTypes, null, batchNoMin, batchNoMax, hasDismantleRecord
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
    public ResponseEntity<VehicleModel> get(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean signMedia
    ) {
        long totalStart = System.nanoTime();
        log.debug("Vehicle detail request, vehicleId={}, signMedia={}, storageService={}",
                id, signMedia, fileStorageService.getClass().getSimpleName());
        long loadVehicleStart = System.nanoTime();
        ResponseEntity<VehicleModel> response = vehicleModelRepository.findById(id)
                .map(vehicle -> {
                    long loadVehicleMs = performanceLogSupport.elapsedMillis(loadVehicleStart);
                    long signStart = System.nanoTime();
                    MediaAttachStats stats = signMedia ? attachPresignedUrls(vehicle) : MediaAttachStats.empty();
                    long signMs = performanceLogSupport.elapsedMillis(signStart);
                    long totalMs = performanceLogSupport.elapsedMillis(totalStart);
                    performanceLogSupport.logStep(
                            log,
                            "vehicle-detail vehicleId=" + id,
                            totalMs,
                            "loadVehicleMs=" + loadVehicleMs
                                    + ", signMediaMs=" + signMs
                                    + ", signMedia=" + signMedia
                                    + ", imageCount=" + stats.imageCount()
                                    + ", documentCount=" + stats.documentCount()
                    );
                    return ResponseEntity.ok(vehicle);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
        return response;
    }

    @GetMapping("/{id}/same-series")
    public ResponseEntity<SameSeriesResponse> sameSeries(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int yearWindow,
            @RequestParam(defaultValue = "30") int limit
    ) {
        int safeYearWindow = Math.min(Math.max(yearWindow, 1), 8);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return sameSeriesService.findSameSeries(id, safeYearWindow, safeLimit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private MediaAttachStats attachPresignedUrls(VehicleModel vehicle) {
        int imageCount = 0;
        if (vehicle.getImages() != null) {
            vehicle.getImages().forEach(image -> {
                if (image.getImageUrl() != null && !image.getImageUrl().trim().isEmpty()) {
                    image.setImageUrl(fileStorageService.generatePresignedUrl(image.getImageUrl(), 3600));
                }
            });
            imageCount = vehicle.getImages().size();
        }
        int documentCount = 0;
        if (vehicle.getDocuments() != null) {
            vehicle.getDocuments().forEach(doc -> {
                if (doc.getDocUrl() != null && !doc.getDocUrl().trim().isEmpty()) {
                    doc.setDocUrl(fileStorageService.generatePresignedUrl(doc.getDocUrl(), 3600));
                }
            });
            documentCount = vehicle.getDocuments().size();
        }
        return new MediaAttachStats(imageCount, documentCount);
    }

    private record MediaAttachStats(int imageCount, int documentCount) {
        private static MediaAttachStats empty() {
            return new MediaAttachStats(0, 0);
        }
    }
}
