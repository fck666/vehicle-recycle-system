package com.scrap_system.backend_api.specification;

import com.scrap_system.backend_api.model.VehicleModel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class VehicleSpecs {

    public static Specification<VehicleModel> withDynamicQuery(
            String q,
            List<String> brands,
            List<String> manufacturers,
            List<String> vehicleTypes,
            List<String> fuelTypes,
            List<String> productIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 全局模糊搜索 (q) - 与其他条件是 AND 关系
            if (StringUtils.hasText(q)) {
                String likePattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("brand")), likePattern),
                        cb.like(cb.lower(root.get("model")), likePattern),
                        cb.like(cb.lower(root.get("vehicleType")), likePattern),
                        cb.like(cb.lower(root.get("fuelType")), likePattern),
                        cb.like(cb.lower(root.get("productId")), likePattern),
                        cb.like(cb.lower(root.get("productNo")), likePattern)
                ));
            }

            // 2. 品牌多选 (OR)
            if (brands != null && !brands.isEmpty()) {
                predicates.add(root.get("brand").in(brands));
            }

            // 3. 企业多选 (OR)
            if (manufacturers != null && !manufacturers.isEmpty()) {
                predicates.add(root.get("manufacturerName").in(manufacturers));
            }

            // 4. 车辆类型多选 (OR)
            if (vehicleTypes != null && !vehicleTypes.isEmpty()) {
                predicates.add(root.get("vehicleType").in(vehicleTypes));
            }

            // 5. 燃油类型多选 (OR)
            if (fuelTypes != null && !fuelTypes.isEmpty()) {
                predicates.add(root.get("fuelType").in(fuelTypes));
            }
            
            // 6. 产品型号多选 (OR) - 这里假设 productId 列表用于精确匹配多个ID
            if (productIds != null && !productIds.isEmpty()) {
                predicates.add(root.get("productId").in(productIds));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
