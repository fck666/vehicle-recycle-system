package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.ExternalVehicleTrim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalVehicleTrimRepository extends JpaRepository<ExternalVehicleTrim, Long> {
    Optional<ExternalVehicleTrim> findBySourceAndSourceTrimId(String source, String sourceTrimId);

    @Query("""
            select t from ExternalVehicleTrim t
            where (:q is null
               or lower(t.brand) like lower(concat('%', :q, '%'))
               or lower(coalesce(t.seriesName, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(t.marketName, '')) like lower(concat('%', :q, '%'))
            )
            """)
    Page<ExternalVehicleTrim> search(@Param("q") String q, Pageable pageable);
}

