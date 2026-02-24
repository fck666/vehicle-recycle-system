package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.MaterialPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MaterialPriceRepository extends JpaRepository<MaterialPrice, Long> {
    Optional<MaterialPrice> findByType(String type);
}
