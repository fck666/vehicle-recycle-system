package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "role", indexes = {@Index(name = "idx_role_code", columnList = "code", unique = true)})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 32, nullable = false, unique = true)
    private String code;
}

