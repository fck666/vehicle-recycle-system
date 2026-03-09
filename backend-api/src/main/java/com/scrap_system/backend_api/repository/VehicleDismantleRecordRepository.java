package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleDismantleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleDismantleRecordRepository extends JpaRepository<VehicleDismantleRecord, Long> {
    List<VehicleDismantleRecord> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);
}
