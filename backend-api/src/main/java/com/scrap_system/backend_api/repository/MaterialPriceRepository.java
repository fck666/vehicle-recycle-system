package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.MaterialPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialPriceRepository extends JpaRepository<MaterialPrice, Long> {
    Optional<MaterialPrice> findByTypeAndEffectiveDate(String type, LocalDate effectiveDate);

    Optional<MaterialPrice> findFirstByTypeOrderByEffectiveDateDescFetchedAtDesc(String type);

    List<MaterialPrice> findByTypeAndEffectiveDateBetweenOrderByEffectiveDateDesc(String type, LocalDate from, LocalDate to);

    List<MaterialPrice> findByTypeOrderByEffectiveDateDescFetchedAtDesc(String type);

    void deleteByType(String type);

    @Query("""
            select mp from MaterialPrice mp
            where mp.effectiveDate = (
                select max(x.effectiveDate) from MaterialPrice x where x.type = mp.type
            )
            order by mp.type asc
            """)
    List<MaterialPrice> findLatestPerType();

    @Query("""
            select mp from MaterialPrice mp
            where mp.type = :type and mp.effectiveDate <= :date
            order by mp.effectiveDate desc, mp.fetchedAt desc
            """)
    List<MaterialPrice> findLatestOnOrBefore(@Param("type") String type, @Param("date") LocalDate date);
}
