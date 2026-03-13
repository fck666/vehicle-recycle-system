package com.scrap_system.backend_api.service.impl;

import com.scrap_system.backend_api.service.FileStorageService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Profile("!prod & !local-prod")
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public LocalFileStorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String subPath) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            String originalFilename = file.getOriginalFilename();
            String safeOriginalFilename = originalFilename == null ? "file" : Paths.get(originalFilename).getFileName().toString();
            safeOriginalFilename = safeOriginalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

            String normalizedSubPath = subPath == null ? "" : subPath.trim();
            normalizedSubPath = normalizedSubPath.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");

            Path targetDir = normalizedSubPath.isBlank()
                    ? rootLocation
                    : rootLocation.resolve(normalizedSubPath).normalize();

            if (!targetDir.startsWith(rootLocation)) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            Files.createDirectories(targetDir);

            String filename = UUID.randomUUID() + "_" + safeOriginalFilename;
            Path destinationFile = targetDir.resolve(filename).normalize();
            if (!destinationFile.startsWith(rootLocation)) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            if (normalizedSubPath.isBlank()) {
                return "/uploads/" + filename;
            }
            return "/uploads/" + normalizedSubPath + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    @Override
    public void deleteFile(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        try {
            String relativePath = url.substring("/uploads/".length());
            // Protect against path traversal
            if (relativePath.contains("..")) {
                return;
            }
            Path file = rootLocation.resolve(relativePath).normalize();
            if (!file.startsWith(rootLocation)) {
                return;
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Ignore error
        }
    }

    @Override
    public String generatePresignedUrl(String url, int expirationSeconds) {
        return url;
    }
}
