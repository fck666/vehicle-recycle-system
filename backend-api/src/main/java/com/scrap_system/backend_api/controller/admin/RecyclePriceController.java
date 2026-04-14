package com.scrap_system.backend_api.controller.admin;

import com.scrap_system.backend_api.dto.admin.RecyclePriceImportDto;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.service.RecyclePriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecyclePriceController {

    private final RecyclePriceService recyclePriceService;

    @GetMapping("/admin/recycle-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MaterialPrice>> getRecyclePricesAdmin() {
        return ResponseEntity.ok(recyclePriceService.getRecyclePrices());
    }

    @GetMapping("/admin/recycle-prices/types")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<String>> getRecycleMaterialTypes() {
        return ResponseEntity.ok(recyclePriceService.getRecycleMaterialTypes());
    }

    @PostMapping("/admin/recycle-prices/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importRecyclePrices(@RequestParam("file") MultipartFile file) {
        try {
            recyclePriceService.importRecyclePrices(file);
            return ResponseEntity.ok("Import successful");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Import failed: " + e.getMessage());
        }
    }

    @PostMapping("/admin/recycle-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> upsertRecyclePrice(@RequestBody RecyclePriceImportDto dto) {
        try {
            recyclePriceService.saveRecyclePrice(dto);
            return ResponseEntity.ok("Save successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Save failed: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/admin/recycle-prices/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteRecyclePriceType(@PathVariable String type) {
        try {
            recyclePriceService.deleteRecyclePriceType(type);
            return ResponseEntity.ok("Delete successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Delete failed: " + e.getMessage());
        }
    }
    
    // Public endpoint for miniprogram/frontend
    @GetMapping("/recycle-prices")
    public ResponseEntity<List<MaterialPrice>> getRecyclePricesPublic() {
        return ResponseEntity.ok(recyclePriceService.getRecyclePrices());
    }

}
