package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "material_template")
public class MaterialTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_type", length = 32)
    private String vehicleType;

    @Column(name = "scope_type", nullable = false, length = 16)
    private String scopeType;

    @Column(name = "scope_value", nullable = false, length = 64)
    private String scopeValue;

    @Column(name = "others_price_per_kg_override", precision = 10, scale = 2)
    private BigDecimal othersPricePerKgOverride;

    @Column(name = "steel_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal steelRatio;

    @Column(name = "aluminum_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal aluminumRatio;

    @Column(name = "copper_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal copperRatio;

    @Column(name = "recovery_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal recoveryRatio;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
