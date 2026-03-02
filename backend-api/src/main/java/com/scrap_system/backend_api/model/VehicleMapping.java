package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vehicle_mapping",
        indexes = {
                @Index(name = "idx_vehicle_mapping_status", columnList = "status"),
                @Index(name = "idx_vehicle_mapping_ext_id", columnList = "external_trim_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vehicle_mapping_miit", columnNames = {"miit_vehicle_id"})
        })
public class VehicleMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "miit_vehicle_id", nullable = false)
    private Long miitVehicleId;

    @Column(name = "external_trim_id")
    private Long externalTrimId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "score")
    private Double score;

    @Column(name = "matched_by", length = 64)
    private String matchedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

