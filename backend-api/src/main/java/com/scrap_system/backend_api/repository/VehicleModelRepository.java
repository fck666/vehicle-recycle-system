package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    Optional<VehicleModel> findByProductId(String productId);
    Optional<VehicleModel> findByProductNo(String productNo);
}
