package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.ComponentDict;
import com.scrap_system.backend_api.repository.ComponentDictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ComponentDictController {

    private final ComponentDictRepository componentDictRepository;
    private static final List<String> DEFAULT_COMPONENT_NAMES = List.of(
            "三元催化", "发动机", "变速箱", "轮毂", "电机", "空调压缩机", "发电机", "音响", "中控", "座椅",
            "电瓶", "方向盘", "转向机", "水箱", "水箱盖", "风扇", "ABS", "录音机", "仪表", "雨刷", "暖风电机", "天窗", "油箱"
    );

    // Public endpoint for miniprogram to get enabled components
    @GetMapping("/components")
    public ResponseEntity<List<ComponentDict>> getEnabledComponents() {
        try {
            List<ComponentDict> components = componentDictRepository.findByIsEnabledTrueOrderBySortOrderAscIdAsc();
            return ResponseEntity.ok((components == null || components.isEmpty()) ? buildDefaultComponents() : components);
        } catch (RuntimeException ex) {
            return ResponseEntity.ok(buildDefaultComponents());
        }
    }

    // Admin endpoints
    @GetMapping("/admin/components")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<ComponentDict>> getAllComponents() {
        return ResponseEntity.ok(componentDictRepository.findAllByOrderBySortOrderAscIdAsc());
    }

    @PostMapping("/admin/components")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComponentDict> createComponent(@RequestBody ComponentDict component) {
        if (component.getName() == null || component.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        component.setName(component.getName().trim());
        if (component.getSortOrder() == null) {
            component.setSortOrder(0);
        }
        if (component.getIsEnabled() == null) {
            component.setIsEnabled(true);
        }
        return ResponseEntity.ok(componentDictRepository.save(component));
    }

    @PutMapping("/admin/components/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComponentDict> updateComponent(@PathVariable Long id, @RequestBody ComponentDict component) {
        return componentDictRepository.findById(id).map(existing -> {
            if (component.getName() != null && !component.getName().trim().isEmpty()) {
                existing.setName(component.getName().trim());
            }
            if (component.getSortOrder() != null) {
                existing.setSortOrder(component.getSortOrder());
            }
            if (component.getIsEnabled() != null) {
                existing.setIsEnabled(component.getIsEnabled());
            }
            return ResponseEntity.ok(componentDictRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/components/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteComponent(@PathVariable Long id) {
        if (componentDictRepository.existsById(id)) {
            componentDictRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private List<ComponentDict> buildDefaultComponents() {
        List<ComponentDict> defaults = new ArrayList<>();
        for (int i = 0; i < DEFAULT_COMPONENT_NAMES.size(); i++) {
            ComponentDict item = new ComponentDict();
            item.setName(DEFAULT_COMPONENT_NAMES.get(i));
            item.setSortOrder((i + 1) * 10);
            item.setIsEnabled(true);
            defaults.add(item);
        }
        return defaults;
    }
}
