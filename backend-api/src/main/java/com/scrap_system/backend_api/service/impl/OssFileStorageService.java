package com.scrap_system.backend_api.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.ResponseHeaderOverrides;
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
            
            // Set content type to ensure correct browser handling
            ObjectMetadata metadata = new ObjectMetadata();
            String contentType = getContentType(fileName);
            if (contentType == null && file.getContentType() != null) {
                contentType = file.getContentType();
            }
            if (contentType != null) {
                metadata.setContentType(contentType);
            }
            metadata.setContentLength(file.getSize());
            
            ossClient.putObject(bucketName, objectKey, file.getInputStream(), metadata);

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

    @Override
    public String generatePresignedUrl(String url, int expirationSeconds) {
        String key = extractKeyFromUrl(url);
        if (key == null) return url;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + expirationSeconds * 1000L);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setExpiration(expiration);
            
            // Set response headers to force inline display
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            responseHeaders.setContentDisposition("inline");
            
            // The Content-Type should be set correctly during upload
            // Fix: Check and auto-fix Content-Type if needed
            String expectedType = getContentType(key);
            if (expectedType != null) {
                try {
                    ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, key);
                    String currentType = meta.getContentType();
                    
                    boolean needFix = false;
                    
                    // Strict check for HTML files
                    if (key.toLowerCase().endsWith(".html") || key.toLowerCase().endsWith(".htm")) {
                        if (!"text/html".equalsIgnoreCase(currentType)) {
                            needFix = true;
                            expectedType = "text/html";
                        }
                    } else {
                        // General check for other types
                        needFix = currentType == null || currentType.trim().isEmpty() 
                                || "application/octet-stream".equalsIgnoreCase(currentType);
                    }

                    if (needFix) {
                        log.info("Auto-fixing Content-Type for key={} from '{}' to '{}'", key, currentType, expectedType);
                        
                        // Copy object to itself with new metadata
                        ObjectMetadata newMeta = new ObjectMetadata();
                        newMeta.setContentType(expectedType);
                        // Also explicitly set Content-Disposition to inline in metadata to be safe
                        newMeta.setContentDisposition("inline");
                        
                        // Preserve original user metadata if needed (optional, here we prioritize fixing type)
                        
                        CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, key, bucketName, key);
                        copyRequest.setNewObjectMetadata(newMeta);
                        
                        ossClient.copyObject(copyRequest);
                        log.info("Successfully fixed Content-Type for key={}", key);
                    }
                } catch (Exception e) {
                    log.warn("Failed to auto-fix Content-Type for key={}", key, e);
                }
            }
            
            request.setResponseHeaders(responseHeaders);
            
            java.net.URL signedUrl = ossClient.generatePresignedUrl(request);
            return signedUrl.toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            return url;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String getContentType(String key) {
        if (key == null) return null;
        String lowerKey = key.toLowerCase();
        if (lowerKey.endsWith(".html") || lowerKey.endsWith(".htm")) return "text/html";
        if (lowerKey.endsWith(".pdf")) return "application/pdf";
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) return "image/jpeg";
        if (lowerKey.endsWith(".png")) return "image/png";
        if (lowerKey.endsWith(".gif")) return "image/gif";
        if (lowerKey.endsWith(".txt")) return "text/plain";
        return null; // Let OSS decide or use default
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
