package com.scrap_system.backend_api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

@RestController
@RequestMapping({"/api/vehicle-images", "/api/vehicles"})
@Slf4j
public class VehicleImageProxyController {
    private static final String OSS_ALLOWED_HOST = "xhy-car-files.oss-cn-beijing.aliyuncs.com";

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam String source) {
        return proxyInternal(source);
    }

    @GetMapping("/image-proxy")
    public ResponseEntity<byte[]> imageProxy(@RequestParam String source) {
        return proxyInternal(source);
    }

    private ResponseEntity<byte[]> proxyInternal(String source) {
        try {
            URI uri = URI.create(source);
            String host = uri.getHost();
            String scheme = uri.getScheme();
            if (host == null || scheme == null || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))) {
                return ResponseEntity.badRequest().build();
            }
            if (!OSS_ALLOWED_HOST.equalsIgnoreCase(host)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                log.warn("Image proxy upstream failed, status={}, source={}", status, source);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }
            byte[] bytes;
            try (InputStream in = connection.getInputStream()) {
                bytes = in.readAllBytes();
            }
            log.info("Image proxy success, host={}, bytes={}", host, bytes.length);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (Exception e) {
            log.warn("Image proxy failed, source={}", source, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
