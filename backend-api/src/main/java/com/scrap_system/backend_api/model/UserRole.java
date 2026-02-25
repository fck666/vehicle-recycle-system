package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
        name = "user_role",
        uniqueConstraints = {@UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role_id"})},
        indexes = {
                @Index(name = "idx_user_role_user_id", columnList = "user_id"),
                @Index(name = "idx_user_role_role_id", columnList = "role_id")
        }
)
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;
}

