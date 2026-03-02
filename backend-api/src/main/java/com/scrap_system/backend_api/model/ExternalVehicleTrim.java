package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "external_vehicle_trim",
        indexes = {
                @Index(name = "idx_ext_trim_source_id", columnList = "source,source_trim_id"),
                @Index(name = "idx_ext_trim_brand_series_year", columnList = "brand,series_name,model_year")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ext_trim_source_id", columnNames = {"source", "source_trim_id"})
        })
public class ExternalVehicleTrim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "source_trim_id", nullable = false, length = 64)
    private String sourceTrimId;

    @Column(nullable = false, length = 64)
    private String brand;

    @Column(name = "series_name", length = 128)
    private String seriesName;

    @Column(name = "market_name", length = 255)
    private String marketName;

    @Column(name = "model_year")
    private Integer modelYear;

    @Column(name = "energy_type", length = 32)
    private String energyType;

    @Column(name = "official_price", precision = 10, scale = 2)
    private BigDecimal officialPrice;

    @Column(name = "battery_kwh", precision = 10, scale = 2)
    private BigDecimal batteryKwh;

    @Column(name = "displacement_ml")
    private Integer displacementMl;

    @Column(name = "power_kw", precision = 10, scale = 2)
    private BigDecimal powerKw;

    @Column(name = "curb_weight", precision = 10, scale = 2)
    private BigDecimal curbWeight;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    @Column(name = "page_url", length = 512)
    private String pageUrl;

    @Lob
    @Column(name = "raw_json", columnDefinition = "LONGTEXT")
    private String rawJson;

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

