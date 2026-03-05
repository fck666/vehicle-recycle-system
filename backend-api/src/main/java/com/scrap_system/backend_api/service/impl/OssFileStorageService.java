package com.scrap_system.backend_api.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.scrap_system.backend_api.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Profile("prod")
@Slf4j
public class OssFileStorageService implements FileStorageService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;
    
    // Optional: Public domain for CDN/Custom Domain
    @Value("${aliyun.oss.domain:}")
    private String domain;

    @Override
    public String uploadFile(MultipartFile file, String path) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // Generate unique filename
            String fileName = UUID.randomUUID().toString() + extension;
            
            // Build object key (path + filename)
            // path usually is "vehicles/123"
            String objectKey = path + "/" + fileName;
            
            // Normalize path (remove leading slash)
            if (objectKey.startsWith("/")) {
                objectKey = objectKey.substring(1);
            }

            log.info("Uploading file to OSS: bucket={}, key={}", bucketName, objectKey);
            ossClient.putObject(bucketName, objectKey, file.getInputStream());

            // Build return URL
            if (domain != null && !domain.isEmpty()) {
                // If custom domain is configured (e.g. https://cdn.example.com)
                return domain.endsWith("/") ? domain + objectKey : domain + "/" + objectKey;
            } else {
                // Default OSS URL: https://bucket.endpoint/key
                // Note: endpoint might contain http/https prefix
                String protocol = endpoint.startsWith("https://") ? "https://" : "http://";
                String ep = endpoint.replace("http://", "").replace("https://", "");
                return protocol + bucketName + "." + ep + "/" + objectKey;
            }
            
        } catch (IOException e) {
            log.error("Failed to upload file to OSS", e);
            throw new RuntimeException("Failed to upload file to OSS", e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public void deleteFile(String url) {
        String key = extractKeyFromUrl(url);
        if (key == null) return;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            log.error("Failed to delete file from OSS", e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String extractKeyFromUrl(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            String path = u.getPath();
            if (path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }
}
