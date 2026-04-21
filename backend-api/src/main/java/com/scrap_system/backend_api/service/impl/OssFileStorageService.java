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
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
@Profile("prod | local-prod")
@Slf4j
public class OssFileStorageService implements FileStorageService {

    private static final long PRESIGNED_URL_SAFETY_WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, CachedSignedUrl> presignedUrlCache = new ConcurrentHashMap<>();

    @Value("${aliyun.oss.endpoint:${spring.aliyun.oss.endpoint:}}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id:${spring.aliyun.oss.access-key-id:}}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:${spring.aliyun.oss.access-key-secret:}}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name:${spring.aliyun.oss.bucket-name:}}")
    private String bucketName;
    
    // Optional: Public domain for CDN/Custom Domain
    @Value("${aliyun.oss.domain:${spring.aliyun.oss.domain:}}")
    private String domain;

    private boolean isOssConfigured() {
        return endpoint != null && !endpoint.isBlank()
                && accessKeyId != null && !accessKeyId.isBlank()
                && accessKeySecret != null && !accessKeySecret.isBlank()
                && bucketName != null && !bucketName.isBlank();
    }

    @Override
    public String uploadFile(MultipartFile file, String path) {
        if (!isOssConfigured()) {
            throw new RuntimeException("OSS 未配置完整，缺少 endpoint/access-key-id/access-key-secret/bucket-name");
        }
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
            // Force inline display
            metadata.setContentDisposition("inline");
            // Cache for 30 days
            metadata.setCacheControl("max-age=2592000");
            
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
        if (!isOssConfigured()) {
            log.warn("OSS 未配置完整，跳过删除文件: {}", url);
            return;
        }
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
        if (!isOssConfigured()) {
            log.warn("OSS not configured when generating presigned url, rawUrl={}", url);
            return url;
        }
        String key = extractKeyFromUrl(url);
        if (key == null || key.isBlank()) {
            log.warn("Cannot extract oss key from url, rawUrl={}", url);
            return url;
        }

        String cacheKey = key + "|" + expirationSeconds;
        CachedSignedUrl cached = presignedUrlCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAtMs() > now) {
            return cached.url();
        }

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            java.util.Date expiration = new java.util.Date(now + expirationSeconds * 1000L);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setExpiration(expiration);
            
            // Set response headers to force inline display
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            responseHeaders.setContentDisposition("inline");
            request.setResponseHeaders(responseHeaders);
            
            java.net.URL signedUrl = ossClient.generatePresignedUrl(request);
            String normalized = normalizeForMiniProgram(signedUrl.toString());
            long ttlMs = Math.max(1_000L, expirationSeconds * 1000L - PRESIGNED_URL_SAFETY_WINDOW_MS);
            presignedUrlCache.put(cacheKey, new CachedSignedUrl(normalized, now + ttlMs));
            evictExpiredPresignedUrls(now);
            return normalized;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            return url;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public void fixImageMetadata(String url) {
        if (!isOssConfigured()) {
            log.warn("OSS 未配置完整，跳过修复元数据: {}", url);
            return;
        }
        String key = extractKeyFromUrl(url);
        if (key == null) return;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // Get current metadata
            ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, key);
            String currentType = meta.getContentType();
            String currentDisposition = meta.getContentDisposition();
            String currentCache = meta.getCacheControl();
            
            // Determine expected Content-Type
            String expectedType = getContentType(key);
            if (expectedType == null && currentType != null) expectedType = currentType; // Keep existing if unknown
            
            boolean needFix = false;
            
            // Check Content-Type
            if (expectedType != null && !expectedType.equalsIgnoreCase(currentType)) {
                needFix = true;
            }
            
            // Check Content-Disposition
            if (currentDisposition == null || !currentDisposition.equalsIgnoreCase("inline")) {
                needFix = true;
            }
            
            // Check Cache-Control (optional, but good practice)
            if (currentCache == null || !currentCache.contains("max-age")) {
                needFix = true;
            }

            if (needFix) {
                log.info("Fixing metadata for key={}: Type={}->{}, Disp={}->inline", 
                        key, currentType, expectedType, currentDisposition);
                
                // Copy object to itself with new metadata
                ObjectMetadata newMeta = new ObjectMetadata();
                if (expectedType != null) newMeta.setContentType(expectedType);
                newMeta.setContentDisposition("inline");
                newMeta.setCacheControl("max-age=2592000"); // 30 days
                
                // Preserve user metadata if any (not implemented here for simplicity)
                
                CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, key, bucketName, key);
                copyRequest.setNewObjectMetadata(newMeta);
                
                ossClient.copyObject(copyRequest);
                log.info("Successfully fixed metadata for key={}", key);
            }
        } catch (Exception e) {
            log.error("Failed to fix metadata for key={}", key, e);
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
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return url.startsWith("/") ? url.substring(1) : url;
            }
            java.net.URL u = new java.net.URL(url);
            String path = u.getPath() == null ? "" : u.getPath();
            if (path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (Exception e) {
            log.warn("Failed to parse url for oss key, rawUrl={}", url, e);
            return null;
        }
    }

    private String normalizeForMiniProgram(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        int queryIndex = url.indexOf('?');
        if (queryIndex < 0 || queryIndex >= url.length() - 1) {
            return url;
        }
        String base = url.substring(0, queryIndex + 1);
        String query = url.substring(queryIndex + 1).replace("+", "%2B");
        return base + query;
    }

    private void evictExpiredPresignedUrls(long now) {
        if (presignedUrlCache.size() < 1024) {
            return;
        }
        presignedUrlCache.entrySet().removeIf(entry -> entry.getValue().expiresAtMs() <= now);
    }

    private record CachedSignedUrl(String url, long expiresAtMs) {
    }
}
