package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.MaterialTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MaterialTemplateRepository extends JpaRepository<MaterialTemplate, Long> {
    Optional<MaterialTemplate> findByVehicleType(String vehicleType);
    Optional<MaterialTemplate> findByScopeTypeAndScopeValue(String scopeType, String scopeValue);
}
