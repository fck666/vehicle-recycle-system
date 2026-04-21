package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleDismantleRecord;
import com.scrap_system.backend_api.repository.projection.VehicleValuationSourceView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleDismantleRecordRepository extends JpaRepository<VehicleDismantleRecord, Long> {
    List<VehicleDismantleRecord> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);
    List<VehicleDismantleRecord> findByVehicleIdIn(List<Long> vehicleIds);

    @Query("""
            select
                r.id as id,
                r.vehicleId as vehicleId,
                r.steelWeight as steelWeight,
                r.aluminumWeight as aluminumWeight,
                r.copperWeight as copperWeight,
                r.batteryWeight as batteryWeight,
                r.otherWeight as otherWeight,
                r.detailsJson as detailsJson
            from VehicleDismantleRecord r
            where r.vehicleId in :vehicleIds
            """)
    List<VehicleValuationSourceView> findValuationSourcesByVehicleIdIn(@Param("vehicleIds") List<Long> vehicleIds);
}
