package com.scrap_system.backend_api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vehicle_image")
public class VehicleImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @ToString.Exclude
    @JsonBackReference
    private VehicleModel vehicle;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_name", length = 128)
    private String imageName;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
