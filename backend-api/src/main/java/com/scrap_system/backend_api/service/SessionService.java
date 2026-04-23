package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.model.UserSession;
import com.scrap_system.backend_api.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final SessionCache sessionCache;

    @Value("${app.security.jwt.ttl-seconds:86400}")
    private long ttlSeconds;

    private String getRedisKey(Long userId, String clientType) {
        return "auth:session:" + userId + ":" + clientType;
    }

    @Transactional
    public UserSession issue(Long userId, String clientType) {
        String ct = normalizeClientType(clientType);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exp = now.plusSeconds(ttlSeconds);
        String sessionId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();

        // Save to DB
        UserSession s = userSessionRepository.findByUserIdAndClientType(userId, ct).orElseGet(UserSession::new);
        s.setUserId(userId);
        s.setClientType(ct);
        s.setSessionId(sessionId);
        s.setTokenId(tokenId);
        s.setIssuedAt(now);
        s.setExpiresAt(exp);
        s.setRevokedAt(null);
        UserSession saved = userSessionRepository.save(s);

        String redisKey = getRedisKey(userId, ct);
        String redisValue = sessionId + ":" + tokenId;
        sessionCache.put(redisKey, redisValue, ttlSeconds);

        return saved;
    }

    @Transactional(readOnly = true)
    public boolean isTokenActive(Long userId, String clientType, String sessionId, String tokenId) {
        if (userId == null) return false;
        String ct = normalizeClientType(clientType);
        String redisKey = getRedisKey(userId, ct);
        
        // Check Redis first
        Optional<String> cached = sessionCache.get(redisKey);
        if (cached.isPresent()) {
            String expected = sessionId + ":" + tokenId;
            return cached.get().equals(expected);
        }

        // Fallback to DB
        return userSessionRepository.findByUserIdAndClientType(userId, ct)
                .filter(s -> s.getRevokedAt() == null)
                .filter(s -> s.getExpiresAt() != null && s.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(s -> sessionId != null && sessionId.equals(s.getSessionId()))
                .filter(s -> tokenId != null && tokenId.equals(s.getTokenId()))
                .map(s -> {
                    // Re-populate Redis if valid (Best Effort)
                    long remainingTtl = java.time.Duration.between(LocalDateTime.now(), s.getExpiresAt()).getSeconds();
                    if (remainingTtl > 0) {
                        String redisValue = s.getSessionId() + ":" + s.getTokenId();
                        sessionCache.put(redisKey, redisValue, remainingTtl);
                    }
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void revoke(Long userId, String clientType) {
        if (userId == null) return;
        String ct = normalizeClientType(clientType);
        
        // Remove from Redis (Best Effort)
        String redisKey = getRedisKey(userId, ct);
        sessionCache.delete(redisKey);

        // Update DB
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
