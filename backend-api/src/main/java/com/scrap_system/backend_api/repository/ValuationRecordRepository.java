package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.ValuationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ValuationRecordRepository extends JpaRepository<ValuationRecord, Long> {
    List<ValuationRecord> findByVehicleIdOrderByCreatedTimeDesc(Long vehicleId);
}
