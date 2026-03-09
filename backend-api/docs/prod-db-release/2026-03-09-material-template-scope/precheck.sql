SELECT @@version AS mysql_version;

SELECT
  COUNT(*) AS duplicated_scope_count
FROM (
  SELECT scope_type, scope_value, COUNT(*) AS c
  FROM material_template
  WHERE scope_type IS NOT NULL AND scope_value IS NOT NULL
  GROUP BY scope_type, scope_value
  HAVING COUNT(*) > 1
) t;

SELECT
  COUNT(*) AS empty_vehicle_type_count
FROM material_template
WHERE (vehicle_type IS NULL OR TRIM(vehicle_type) = '');

SELECT
  COUNT(*) AS null_scope_value_after_backfill
FROM material_template
WHERE (scope_value IS NULL OR TRIM(scope_value) = '');

SELECT
  COUNT(*) AS others_rows_without_price
FROM material_template_item i
JOIN material_template t ON t.id = i.template_id
WHERE i.material_type = 'others'
  AND t.others_price_per_kg_override IS NULL;
