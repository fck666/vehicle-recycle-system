package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vehicle_mapping_candidate",
        indexes = {
                @Index(name = "idx_vmc_miit_rank", columnList = "miit_vehicle_id,rank_no"),
                @Index(name = "idx_vmc_ext", columnList = "external_trim_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vmc_miit_rank", columnNames = {"miit_vehicle_id", "rank_no"})
        })
public class VehicleMappingCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "miit_vehicle_id", nullable = false)
    private Long miitVehicleId;

    @Column(name = "external_trim_id", nullable = false)
    private Long externalTrimId;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    @Column(name = "matched_by", length = 64)
    private String matchedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

