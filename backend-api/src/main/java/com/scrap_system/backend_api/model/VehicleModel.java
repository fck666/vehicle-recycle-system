package com.scrap_system.backend_api.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.scrap_system.backend_api.model.enums.VehicleSourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicle_model", indexes = {
    @Index(name = "idx_brand", columnList = "brand"),
    @Index(name = "idx_model", columnList = "model"),
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_product_no", columnList = "product_no"),
    @Index(name = "idx_batch_no", columnList = "batch_no")
})
@Getter
@Setter
public class VehicleModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Source Info
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20, nullable = false)
    private VehicleSourceType sourceType = VehicleSourceType.MANUAL;

    @Column(nullable = false, length = 64)
    private String brand;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    @Column(name = "fuel_type", nullable = false, length = 32)
    private String fuelType;

    @Column(name = "product_no", length = 64)
    private String productNo;

    @Column(name = "product_id", length = 64)
    private String productId;

    @Column(name = "product_model", length = 255)
    private String productModel;

    @Column(name = "manufacturer_name", length = 255)
    private String manufacturerName;

    @Column(name = "trademark", length = 128)
    private String trademark;

    @Column(name = "production_address", length = 255)
    private String productionAddress;

    @Column(name = "registration_address", length = 255)
    private String registrationAddress;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "batch_no")
    private Integer batchNo;

    @Column(name = "catalog_index")
    private Integer catalogIndex;

    @Column(name = "length_mm")
    private Integer lengthMm;

    @Column(name = "width_mm")
    private Integer widthMm;

    @Column(name = "height_mm")
    private Integer heightMm;

    @Column(name = "wheelbase_mm")
    private Integer wheelbaseMm;

    @Column(name = "front_overhang_mm")
    private Integer frontOverhangMm;

    @Column(name = "rear_overhang_mm")
    private Integer rearOverhangMm;

    @Column(name = "approach_angle_deg", precision = 6, scale = 2)
    private BigDecimal approachAngleDeg;

    @Column(name = "departure_angle_deg", precision = 6, scale = 2)
    private BigDecimal departureAngleDeg;

    @Column(name = "front_track_mm")
    private Integer frontTrackMm;

    @Column(name = "rear_track_mm")
    private Integer rearTrackMm;

    @Column(name = "axle_count")
    private Integer axleCount;

    @Column(name = "axle_load_kg", length = 64)
    private String axleLoadKg;

    @Column(name = "tire_count")
    private Integer tireCount;

    @Column(name = "tire_spec", length = 255)
    private String tireSpec;

    @Column(name = "steering_type", length = 64)
    private String steeringType;

    @Column(name = "has_abs")
    private Boolean hasAbs;

    @Column(name = "max_speed_kmh")
    private Integer maxSpeedKmh;

    @Column(name = "curb_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal curbWeight;

    @Column(name = "gross_weight", precision = 10, scale = 2)
    private BigDecimal grossWeight;

    @Column(name = "battery_kwh", precision = 10, scale = 2)
    private BigDecimal batteryKwh;

    @Column(name = "motor_model", length = 128)
    private String motorModel;

    @Column(name = "motor_manufacturer", length = 255)
    private String motorManufacturer;

    @Column(name = "displacement_ml")
    private Integer displacementMl;

    @Column(name = "power_kw", precision = 10, scale = 2)
    private BigDecimal powerKw;

    @Column(name = "vin_pattern", length = 64)
    private String vinPattern;

    @Column(name = "chassis_model", length = 128)
    private String chassisModel;

    @Lob
    @Column(name = "spec_raw_json", columnDefinition = "LONGTEXT")
    private String specRawJson;

    @Column(name = "vehicle_type", nullable = false, length = 32)
    private String vehicleType;

    @Column(name = "source_site", length = 32)
    private String sourceSite;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @ToString.Exclude
    @JsonManagedReference
    @BatchSize(size = 50)
    private List<VehicleImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @ToString.Exclude
    @JsonManagedReference
    @BatchSize(size = 50)
    private List<VehicleDocument> documents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
