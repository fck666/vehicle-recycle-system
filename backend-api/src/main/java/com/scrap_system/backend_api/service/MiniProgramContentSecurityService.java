package com.scrap_system.backend_api.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrap_system.backend_api.exception.ContentSecurityException;
import com.scrap_system.backend_api.model.VehicleDismantleRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MiniProgramContentSecurityService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WxMaService wxMaService;

    @Value("${app.security.content-check.enabled:false}")
    private boolean enabled;

    public void validateUsername(String username) {
        validateText("用户名", username);
    }

    public void validateDismantleRecord(VehicleDismantleRecord record) {
        if (record == null) {
            return;
        }

        validateText("备注信息", record.getRemark());
        for (String text : extractDetailTexts(record.getDetailsJson())) {
            validateText("提交内容", text);
        }
    }

    private void validateText(String fieldName, String text) {
        String normalized = normalize(text);
        if (!enabled || normalized == null) {
            return;
        }

        try {
            boolean safe = wxMaService.getSecurityService().checkMessage(normalized);
            if (!safe) {
                throw new ContentSecurityException(fieldName + "包含违规内容，请修改后再提交");
            }
        } catch (WxErrorException ex) {
            log.warn("Mini program content security check failed for field={}, code={}, message={}",
                    fieldName,
                    ex.getError() == null ? "unknown" : ex.getError().getErrorCode(),
                    ex.getError() == null ? ex.getMessage() : ex.getError().getErrorMsg());
            throw new ContentSecurityException("内容安全校验失败，请稍后重试");
        }
    }

    private List<String> extractDetailTexts(String detailsJson) {
        String normalized = normalize(detailsJson);
        if (normalized == null) {
            return List.of();
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(normalized);
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return List.of();
            }

            List<String> texts = new ArrayList<>();
            for (JsonNode item : items) {
                addText(texts, item.path("partName").asText(null));
                addText(texts, item.path("remark").asText(null));
            }
            return texts;
        } catch (Exception ex) {
            log.warn("Failed to parse dismantle detailsJson for content security check", ex);
            return List.of();
        }
    }

    private static void addText(List<String> texts, String text) {
        String normalized = normalize(text);
        if (normalized != null) {
            texts.add(normalized);
        }
    }

    private static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
