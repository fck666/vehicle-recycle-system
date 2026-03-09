package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vehicle_dismantle_record")
public class VehicleDismantleRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "steel_weight", precision = 10, scale = 2)
    private BigDecimal steelWeight;

    @Column(name = "aluminum_weight", precision = 10, scale = 2)
    private BigDecimal aluminumWeight;

    @Column(name = "copper_weight", precision = 10, scale = 2)
    private BigDecimal copperWeight;

    @Column(name = "battery_weight", precision = 10, scale = 2)
    private BigDecimal batteryWeight;

    @Column(name = "other_weight", precision = 10, scale = 2)
    private BigDecimal otherWeight;

    @Column(name = "details_json", columnDefinition = "LONGTEXT")
    private String detailsJson;

    @Column(name = "operator_name", length = 64)
    private String operatorName;

    @Column(name = "operator_id", length = 64)
    private String operatorId;

    @Column(name = "images_json", columnDefinition = "LONGTEXT")
    private String imagesJson;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
