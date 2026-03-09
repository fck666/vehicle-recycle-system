package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "material_source_config")
public class MaterialSourceConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 512)
    private String sourceUrl;

    @Column(name = "parse_keyword", nullable = false, length = 64)
    private String parseKeyword;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
