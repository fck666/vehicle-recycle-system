package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    Optional<VehicleModel> findByProductId(String productId);
    Optional<VehicleModel> findByProductNo(String productNo);

    @Query("""
            select v from VehicleModel v
            where :q is null
               or lower(v.brand) like concat('%', lower(:q), '%')
               or lower(v.model) like concat('%', lower(:q), '%')
               or lower(v.vehicleType) like concat('%', lower(:q), '%')
               or lower(v.fuelType) like concat('%', lower(:q), '%')
               or lower(coalesce(v.productId, '')) like concat('%', lower(:q), '%')
               or lower(coalesce(v.productNo, '')) like concat('%', lower(:q), '%')
            """)
    Page<VehicleModel> search(@Param("q") String q, Pageable pageable);
}
