package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.VehicleModel;
import com.scrap_system.backend_api.repository.projection.VehicleSeriesSnapshotView;
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
    List<VehicleModel> findAllByProductIdOrderByIdDesc(String productId);
    List<VehicleModel> findAllByProductNoOrderByIdDesc(String productNo);

    @Query("""
            select v.id
            from VehicleModel v
            where v.productNo = :productNo
            order by v.id desc
            """)
    List<Long> findIdsByProductNoOrderByIdDesc(@Param("productNo") String productNo);

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
    
    @Query(value = """
        SELECT v.id 
        FROM vehicle_model v 
        JOIN vehicle_document d ON v.id = d.vehicle_id 
        WHERE d.doc_type = 'MIIT_HTML' 
        GROUP BY v.id 
        HAVING COUNT(DISTINCT d.source_url) > 1
    """, nativeQuery = true)
    List<Object[]> findIdsWithDuplicateSourceUrls();

    @Query("""
            select
                v.id as id,
                v.brand as brand,
                v.model as model,
                v.modelYear as modelYear,
                v.manufacturerName as manufacturerName,
                v.vehicleType as vehicleType,
                v.fuelType as fuelType,
                v.curbWeight as curbWeight,
                v.wheelbaseMm as wheelbaseMm,
                v.trademark as trademark,
                v.productNo as productNo
            from VehicleModel v
            where v.id = :id
            """)
    Optional<VehicleSeriesSnapshotView> findSeriesSnapshotById(@Param("id") Long id);

    @Query(value = """
            select
                v.id as id,
                v.brand as brand,
                v.model as model,
                v.model_year as modelYear,
                v.manufacturer_name as manufacturerName,
                v.vehicle_type as vehicleType,
                v.fuel_type as fuelType,
                v.curb_weight as curbWeight,
                v.wheelbase_mm as wheelbaseMm,
                v.trademark as trademark,
                v.product_no as productNo
            from vehicle_model v
            where v.id <> :targetId
              and v.vehicle_type_ci = :vehicleTypeCi
              and v.fuel_type_ci = :fuelTypeCi
              and v.model_year between :minYear and :maxYear
              and (
                   :manufacturerNameCi is null
                   or :manufacturerNameCi = ''
                   or v.manufacturer_name_ci = :manufacturerNameCi
              )
            """, nativeQuery = true)
    List<VehicleSeriesSnapshotView> findSameSeriesPoolSnapshotsIndexed(
            @Param("targetId") Long targetId,
            @Param("manufacturerNameCi") String manufacturerNameCi,
            @Param("vehicleTypeCi") String vehicleTypeCi,
            @Param("fuelTypeCi") String fuelTypeCi,
            @Param("minYear") Integer minYear,
            @Param("maxYear") Integer maxYear
    );

    @Query("""
            select
                v.id as id,
                v.brand as brand,
                v.model as model,
                v.modelYear as modelYear,
                v.manufacturerName as manufacturerName,
                v.vehicleType as vehicleType,
                v.fuelType as fuelType,
                v.curbWeight as curbWeight,
                v.wheelbaseMm as wheelbaseMm,
                v.trademark as trademark,
                v.productNo as productNo
            from VehicleModel v
            where v.id <> :targetId
              and lower(v.vehicleType) = lower(:vehicleType)
              and lower(v.fuelType) = lower(:fuelType)
              and v.modelYear between :minYear and :maxYear
              and (
                   :manufacturerName is null
                   or length(trim(:manufacturerName)) = 0
                   or lower(coalesce(v.manufacturerName, '')) = lower(:manufacturerName)
              )
            """)
    List<VehicleSeriesSnapshotView> findSameSeriesPoolSnapshotsFallback(
            @Param("targetId") Long targetId,
            @Param("manufacturerName") String manufacturerName,
            @Param("vehicleType") String vehicleType,
            @Param("fuelType") String fuelType,
            @Param("minYear") Integer minYear,
            @Param("maxYear") Integer maxYear
    );
}
