package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.ComponentDict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentDictRepository extends JpaRepository<ComponentDict, Long> {
    List<ComponentDict> findAllByOrderBySortOrderAscIdAsc();
    List<ComponentDict> findByIsEnabledTrueOrderBySortOrderAscIdAsc();
}