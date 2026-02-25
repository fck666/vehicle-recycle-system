package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.model.UserSession;
import com.scrap_system.backend_api.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;

    @Value("${app.security.jwt.ttl-seconds:86400}")
    private long ttlSeconds;

    @Transactional
    public UserSession issue(Long userId, String clientType) {
        String ct = normalizeClientType(clientType);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exp = now.plusSeconds(ttlSeconds);
        String sessionId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();

        UserSession s = userSessionRepository.findByUserIdAndClientType(userId, ct).orElseGet(UserSession::new);
        s.setUserId(userId);
        s.setClientType(ct);
        s.setSessionId(sessionId);
        s.setTokenId(tokenId);
        s.setIssuedAt(now);
        s.setExpiresAt(exp);
        s.setRevokedAt(null);
        return userSessionRepository.save(s);
    }

    @Transactional(readOnly = true)
    public boolean isTokenActive(Long userId, String clientType, String sessionId, String tokenId) {
        if (userId == null) return false;
        String ct = normalizeClientType(clientType);
        return userSessionRepository.findByUserIdAndClientType(userId, ct)
                .filter(s -> s.getRevokedAt() == null)
                .filter(s -> s.getExpiresAt() != null && s.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(s -> sessionId != null && sessionId.equals(s.getSessionId()))
                .filter(s -> tokenId != null && tokenId.equals(s.getTokenId()))
                .isPresent();
    }

    @Transactional
    public void revoke(Long userId, String clientType) {
        if (userId == null) return;
        String ct = normalizeClientType(clientType);
        userSessionRepository.findByUserIdAndClientType(userId, ct).ifPresent(s -> {
            s.setRevokedAt(LocalDateTime.now());
            userSessionRepository.save(s);
        });
    }

    @Transactional
    public void revokeAll(Long userId) {
        revoke(userId, "WEB");
        revoke(userId, "MINIAPP");
        revoke(userId, "SERVICE");
    }

    private static String normalizeClientType(String clientType) {
        String ct = clientType == null ? "" : clientType.trim().toUpperCase();
        if (ct.isEmpty()) return "WEB";
        return ct;
    }
}
