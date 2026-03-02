package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {
}

