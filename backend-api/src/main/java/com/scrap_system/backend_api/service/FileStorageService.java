package com.scrap_system.backend_api.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String path);
    void deleteFile(String url);
    String generatePresignedUrl(String url, int expirationSeconds);
    
    default void fixImageMetadata(String url) {
        // Default implementation does nothing (for local storage)
    }
}
