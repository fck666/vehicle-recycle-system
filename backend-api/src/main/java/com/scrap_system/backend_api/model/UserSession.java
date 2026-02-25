package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "user_session",
        uniqueConstraints = {@UniqueConstraint(name = "uk_user_session_user_client", columnNames = {"user_id", "client_type"})},
        indexes = {
                @Index(name = "idx_user_session_user_client", columnList = "user_id, client_type"),
                @Index(name = "idx_user_session_session_id", columnList = "session_id", unique = true)
        }
)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "client_type", length = 16, nullable = false)
    private String clientType;

    @Column(name = "session_id", length = 64, nullable = false, unique = true)
    private String sessionId;

    @Column(name = "token_id", length = 64, nullable = false)
    private String tokenId;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}

