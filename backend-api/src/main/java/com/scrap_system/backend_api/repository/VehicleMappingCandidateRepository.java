package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleMappingCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleMappingCandidateRepository extends JpaRepository<VehicleMappingCandidate, Long> {
    List<VehicleMappingCandidate> findByMiitVehicleIdOrderByRankNoAsc(Long miitVehicleId);

    List<VehicleMappingCandidate> findByMiitVehicleIdInOrderByMiitVehicleIdAscRankNoAsc(List<Long> miitVehicleIds);

    @Modifying
    @Query("delete from VehicleMappingCandidate c where c.miitVehicleId = :miitVehicleId")
    void deleteByMiitVehicleId(@Param("miitVehicleId") Long miitVehicleId);
}

