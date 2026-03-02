package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String folder = (path == null || path.trim().isEmpty()) ? "misc" : path.trim();
        String url = fileStorageService.uploadFile(file, folder);
        Map<String, Object> out = new HashMap<>();
        out.put("url", url);
        out.put("name", file.getOriginalFilename());
        out.put("size", file.getSize());
        return ResponseEntity.ok(out);
    }
}

