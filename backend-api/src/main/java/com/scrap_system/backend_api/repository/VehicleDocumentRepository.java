package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {
    Optional<VehicleDocument> findFirstByVehicle_IdAndSha256(Long vehicleId, String sha256);
    Optional<VehicleDocument> findFirstByVehicle_IdAndDocUrl(Long vehicleId, String docUrl);
}
