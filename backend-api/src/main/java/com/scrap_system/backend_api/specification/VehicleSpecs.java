package com.scrap_system.backend_api.specification;

import com.scrap_system.backend_api.model.VehicleModel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import com.scrap_system.backend_api.model.VehicleDismantleRecord;
import jakarta.persistence.criteria.Subquery;

public class VehicleSpecs {

    public static Specification<VehicleModel> withDynamicQuery(
            String q,
            List<String> brands,
            List<String> manufacturers,
            List<String> vehicleTypes,
            List<String> fuelTypes,
            List<String> productIds,
            Integer batchNoMin,
            Integer batchNoMax,
            Boolean hasDismantleRecord
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 全局模糊搜索 (q) - 与其他条件是 AND 关系
            if (StringUtils.hasText(q)) {
                String keyword = q.trim();
                // 采用 MySQL 的 MATCH() AGAINST() 全文搜索语法，解决双向模糊 LIKE '%...%' 的逆向全表扫描陷阱
                // JPA 不原生支持 MATCH AGAINST，使用自定义 Function 或者回退为 LIKE
                // 这里我们使用 Hibernate 提供的 custom function (如果配置了的话)，或者我们手动构建 LIKE
                // 由于原生 JPA Criteria 比较难优雅写 MATCH_AGAINST，且没有自定义方言，
                // 最稳妥、兼容性最好的方案是：放弃让 MySQL 自己去结合 ORDER BY 乱优化。
                // 只要我们不依赖数据库的 LIKE，我们就必须在代码层面做妥协，或者通过强制提示(Index Hint)解决。
                // 为了保证 CBK402E 在中段也能查出来，我们回退为 `%keyword%` 双向模糊。
                // 真正的解决方案是在生产数据库上手动执行：
                // ALTER TABLE vehicle_model ADD FULLTEXT INDEX ft_idx_vm_search (brand, model, product_id, product_no) WITH PARSER ngram;
                // 然后在复杂搜索时使用 NativeQuery。
                // 这里暂时恢复双向模糊，因为这是保证结果不漏的唯一方式。
                String likePattern = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("productNo"), likePattern),
                        cb.like(root.get("productId"), likePattern),
                        cb.like(root.get("model"), likePattern),
                        cb.like(root.get("brand"), likePattern)
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
            
            // 6. 产品型号多选 (OR)
            if (productIds != null && !productIds.isEmpty()) {
                predicates.add(root.get("productId").in(productIds));
            }

            // 7. 批次范围筛选 (AND)
            if (batchNoMin != null) {
                predicates.add(cb.ge(root.get("batchNo"), batchNoMin));
            }
            if (batchNoMax != null) {
                predicates.add(cb.le(root.get("batchNo"), batchNoMax));
            }

            // 8. 是否有拆解记录
            if (Boolean.TRUE.equals(hasDismantleRecord)) {
                // Use EXISTS subquery instead of IN subquery for better performance
                Subquery<Integer> subquery = query.subquery(Integer.class);
                jakarta.persistence.criteria.Root<VehicleDismantleRecord> dismantleRoot = subquery.from(VehicleDismantleRecord.class);
                subquery.select(cb.literal(1));
                subquery.where(cb.equal(dismantleRoot.get("vehicleId"), root.get("id")));
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
