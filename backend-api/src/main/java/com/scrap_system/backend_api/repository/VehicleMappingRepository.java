package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleMappingRepository extends JpaRepository<VehicleMapping, Long> {
    Optional<VehicleMapping> findByMiitVehicleId(Long miitVehicleId);

    List<VehicleMapping> findByMiitVehicleIdIn(List<Long> miitVehicleIds);
}

