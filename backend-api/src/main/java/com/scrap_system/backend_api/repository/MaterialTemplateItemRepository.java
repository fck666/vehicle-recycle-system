package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.MaterialTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialTemplateItemRepository extends JpaRepository<MaterialTemplateItem, Long> {
    List<MaterialTemplateItem> findByTemplateIdIn(List<Long> templateIds);
    List<MaterialTemplateItem> findByTemplateIdOrderByIdAsc(Long templateId);
    void deleteByTemplateId(Long templateId);
}
