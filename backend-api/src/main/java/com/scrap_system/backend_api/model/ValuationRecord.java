package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "valuation_record")
public class ValuationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "valuation_result", nullable = false, precision = 12, scale = 2)
    private BigDecimal valuationResult;

    @Column(name = "steel_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal steelValue;

    @Column(name = "aluminum_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal aluminumValue;

    @Column(name = "copper_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal copperValue;

    @Column(name = "battery_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal batteryValue;

    @Column(name = "created_time", insertable = false, updatable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }
}
