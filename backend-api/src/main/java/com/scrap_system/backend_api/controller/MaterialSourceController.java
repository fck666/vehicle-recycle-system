package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.MaterialSourceConfigUpsertRequest;
import com.scrap_system.backend_api.dto.MaterialSourceSuggestResult;
import com.scrap_system.backend_api.model.MaterialSourceConfig;
import com.scrap_system.backend_api.service.MaterialPriceFetchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/material-sources")
@RequiredArgsConstructor
public class MaterialSourceController {
    private final MaterialPriceFetchService materialPriceFetchService;

    @GetMapping
    public List<MaterialSourceConfig> list() {
        return materialPriceFetchService.listSources();
    }

    @GetMapping("/suggest")
    public List<MaterialSourceSuggestResult> suggest(@RequestParam String keyword) {
        return materialPriceFetchService.suggestFromKeyword(keyword);
    }

    @PostMapping
    public ResponseEntity<MaterialSourceConfig> upsert(@RequestBody MaterialSourceConfigUpsertRequest request) {
        if (request == null || isBlank(request.getType()) || isBlank(request.getDisplayName()) || isBlank(request.getSourceUrl())) {
            return ResponseEntity.badRequest().build();
        }
        MaterialSourceConfig input = new MaterialSourceConfig();
        input.setType(request.getType().trim().toLowerCase(Locale.ROOT));
        input.setDisplayName(request.getDisplayName().trim());
        input.setSourceName(isBlank(request.getSourceName()) ? ("生意社-" + request.getDisplayName().trim()) : request.getSourceName().trim());
        input.setSourceUrl(request.getSourceUrl().trim());
        input.setParseKeyword(isBlank(request.getParseKeyword()) ? request.getDisplayName().trim() : request.getParseKeyword().trim());
        input.setEnabled(request.getEnabled() == null || request.getEnabled());
        return ResponseEntity.ok(materialPriceFetchService.upsertSource(input));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
