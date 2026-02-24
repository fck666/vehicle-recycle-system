package com.scrap_system.backend_api.service.impl;

import com.scrap_system.backend_api.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Profile("prod") // 生产环境（未配置 OSS SDK 前暂不生效，这里仅作占位）
public class OssFileStorageService implements FileStorageService {

    @Override
    public String uploadFile(MultipartFile file, String path) {
        // TODO: 引入阿里云 SDK，实现 putObject
        log.info("Mock uploading to OSS: {}", file.getOriginalFilename());
        return "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/" + path + "/" + file.getOriginalFilename();
    }
}
