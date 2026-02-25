package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "user_account",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_wx_openid", columnList = "wx_openid"),
                @Index(name = "idx_user_wx_unionid", columnList = "wx_unionid"),
                @Index(name = "idx_user_phone", columnList = "phone")
        }
)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 64, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "wx_openid", length = 64, unique = true)
    private String wxOpenid;

    @Column(name = "wx_unionid", length = 64, unique = true)
    private String wxUnionid;

    @Column(name = "phone", length = 32, unique = true)
    private String phone;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
