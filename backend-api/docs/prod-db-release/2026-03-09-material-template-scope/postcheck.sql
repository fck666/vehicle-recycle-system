SELECT @@version AS mysql_version;

-- 验证新列是否创建
SELECT
  COUNT(*) AS new_columns_created
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'material_template'
  AND column_name IN ('scope_type', 'scope_value', 'others_price_per_kg_override');

-- 检查是否有重复的 scope 组合
SELECT
  COUNT(*) AS duplicated_scope_count
FROM (
  SELECT scope_type, scope_value, COUNT(*) AS c
  FROM material_template
  WHERE scope_type IS NOT NULL AND scope_value IS NOT NULL
  GROUP BY scope_type, scope_value
  HAVING COUNT(*) > 1
) t;

-- 检查数据回填是否遗漏（除了原始 vehicle_type 为空的行外，scope_value 不应为空）
SELECT
  COUNT(*) AS null_scope_value_after_backfill
FROM material_template
WHERE (vehicle_type IS NOT NULL AND TRIM(vehicle_type) <> '')
  AND (scope_value IS NULL OR TRIM(scope_value) = '');

-- 检查新字段 others_price_per_kg_override 是否可用（抽样）
SELECT id, scope_type, scope_value, others_price_per_kg_override
FROM material_template
LIMIT 5;
