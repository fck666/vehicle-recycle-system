SELECT @@version AS mysql_version;

-- 检查是否有空的 vehicle_type（迁移时会被跳过，需要关注）
SELECT
  COUNT(*) AS empty_vehicle_type_count
FROM material_template
WHERE (vehicle_type IS NULL OR TRIM(vehicle_type) = '');

-- 检查是否有重复的 vehicle_type（迁移后会变成重复的 scope_value，导致唯一索引创建失败）
SELECT
  COUNT(*) AS duplicated_vehicle_type_count
FROM (
  SELECT vehicle_type, COUNT(*) AS c
  FROM material_template
  WHERE vehicle_type IS NOT NULL AND TRIM(vehicle_type) <> ''
  GROUP BY vehicle_type
  HAVING COUNT(*) > 1
) t;
