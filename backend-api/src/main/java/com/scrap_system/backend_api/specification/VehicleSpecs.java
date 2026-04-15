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
    private static final char LIKE_ESCAPE_CHAR = '\\';

    public enum KeywordSearchMode {
        FUZZY_CONTAINS,
        IDENTIFIER_PREFIX
    }

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
        return withDynamicQuery(
                q,
                brands,
                manufacturers,
                vehicleTypes,
                fuelTypes,
                productIds,
                batchNoMin,
                batchNoMax,
                hasDismantleRecord,
                KeywordSearchMode.FUZZY_CONTAINS
        );
    }

    public static Specification<VehicleModel> withDynamicQuery(
            String q,
            List<String> brands,
            List<String> manufacturers,
            List<String> vehicleTypes,
            List<String> fuelTypes,
            List<String> productIds,
            Integer batchNoMin,
            Integer batchNoMax,
            Boolean hasDismantleRecord,
            KeywordSearchMode keywordSearchMode
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 全局模糊搜索 (q) - 与其他条件是 AND 关系
            if (StringUtils.hasText(q)) {
                String keyword = q.trim();
                if (keywordSearchMode == KeywordSearchMode.IDENTIFIER_PREFIX) {
                    String prefixPattern = buildPrefixLikePattern(keyword);
                    predicates.add(cb.or(
                            cb.like(root.get("productNo"), prefixPattern, LIKE_ESCAPE_CHAR),
                            cb.like(root.get("productId"), prefixPattern, LIKE_ESCAPE_CHAR),
                            cb.like(root.get("model"), prefixPattern, LIKE_ESCAPE_CHAR)
                    ));
                } else {
                    String likePattern = buildContainsLikePattern(keyword);
                    predicates.add(cb.or(
                            cb.like(root.get("productNo"), likePattern, LIKE_ESCAPE_CHAR),
                            cb.like(root.get("productId"), likePattern, LIKE_ESCAPE_CHAR),
                            cb.like(root.get("model"), likePattern, LIKE_ESCAPE_CHAR),
                            cb.like(root.get("brand"), likePattern, LIKE_ESCAPE_CHAR)
                    ));
                }
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

    public static boolean isIdentifierLikeQuery(String rawQuery) {
        if (!StringUtils.hasText(rawQuery)) {
            return false;
        }
        String keyword = rawQuery.trim();
        if (keyword.length() < 4 || keyword.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }

        boolean hasDigit = false;
        boolean hasSeparator = false;
        for (int i = 0; i < keyword.length(); i++) {
            char ch = keyword.charAt(i);
            if (Character.isDigit(ch)) {
                hasDigit = true;
                continue;
            }
            if (Character.isLetter(ch)) {
                continue;
            }
            if (ch == '-' || ch == '_' || ch == '/' || ch == '.') {
                hasSeparator = true;
                continue;
            }
            return false;
        }
        return hasDigit || hasSeparator;
    }

    private static String buildContainsLikePattern(String keyword) {
        return "%" + escapeLikePattern(keyword) + "%";
    }

    private static String buildPrefixLikePattern(String keyword) {
        return escapeLikePattern(keyword) + "%";
    }

    private static String escapeLikePattern(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
