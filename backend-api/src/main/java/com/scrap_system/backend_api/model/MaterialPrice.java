package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "material_price")
public class MaterialPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(name = "price_per_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKg;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "unit", length = 16)
    private String unit;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Column(name = "source_name", length = 64)
    private String sourceName;

    @Column(name = "source_url", length = 512)
    private String sourceUrl;

    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
