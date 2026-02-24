package com.scrap_system.backend_api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vehicle_document")
public class VehicleDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @ToString.Exclude
    @JsonBackReference
    private VehicleModel vehicle;

    @Column(name = "doc_type", length = 32)
    private String docType;

    @Column(name = "doc_name", length = 255)
    private String docName;

    @Column(name = "doc_url", nullable = false, length = 512)
    private String docUrl;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "source_url", length = 512)
    private String sourceUrl;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fetchedAt == null) {
            fetchedAt = createdAt;
        }
    }
}

