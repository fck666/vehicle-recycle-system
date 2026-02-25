package com.scrap_system.backend_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    @Value("${app.security.jwt.secret:}")
    private String secret;

    @Value("${app.security.jwt.issuer:vehicle-recycle-system}")
    private String issuer;

    @Value("${app.security.jwt.ttl-seconds:86400}")
    private long ttlSeconds;

    public String issue(Long userId, List<String> roleCodes, String clientType, String sessionId, String tokenId) {
        SecretKey key = secretKey();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .subject(String.valueOf(userId))
                .claim("roles", roleCodes)
                .id(tokenId != null && !tokenId.isBlank() ? tokenId : UUID.randomUUID().toString())
                .claim("ct", clientType == null ? "WEB" : clientType)
                .claim("sid", sessionId)
                .signWith(key)
                .compact();
    }

    public ParsedToken parse(String token) {
        try {
            SecretKey key = secretKey();
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jws.getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            String ct = claims.get("ct", String.class);
            String sid = claims.get("sid", String.class);
            String jti = claims.getId();
            return new ParsedToken(userId, roles == null ? List.of() : roles, ct, sid, jti);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid token", e);
        }
    }

    private SecretKey secretKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.security.jwt.secret is blank");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    public record ParsedToken(Long userId, List<String> roles, String clientType, String sessionId, String tokenId) {
    }
}
