package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.MaterialSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialSourceConfigRepository extends JpaRepository<MaterialSourceConfig, Long> {
    Optional<MaterialSourceConfig> findByType(String type);
    List<MaterialSourceConfig> findByEnabledTrueOrderByTypeAsc();
}
