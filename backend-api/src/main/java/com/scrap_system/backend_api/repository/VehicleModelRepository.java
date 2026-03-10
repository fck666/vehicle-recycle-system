package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long>, JpaSpecificationExecutor<VehicleModel> {
    Optional<VehicleModel> findByProductId(String productId);
    Optional<VehicleModel> findByProductNo(String productNo);

    List<VehicleModel> findByBatchNo(Integer batchNo);
    List<VehicleModel> findByBatchNoBetween(Integer start, Integer end);

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

    @Query("select distinct v.vehicleType from VehicleModel v where v.vehicleType is not null and length(trim(v.vehicleType)) > 0 order by v.vehicleType asc")
    List<String> findDistinctVehicleTypes();

    @Query("select distinct v.brand from VehicleModel v where v.brand is not null and length(trim(v.brand)) > 0 order by v.brand asc")
    List<String> findDistinctBrands();

    @Query("select distinct v.manufacturerName from VehicleModel v where v.manufacturerName is not null and length(trim(v.manufacturerName)) > 0 order by v.manufacturerName asc")
    List<String> findDistinctManufacturers();

    @Query("select distinct v.fuelType from VehicleModel v where v.fuelType is not null and length(trim(v.fuelType)) > 0 order by v.fuelType asc")
    List<String> findDistinctFuelTypes();
}
