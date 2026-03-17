# 同车系只读 SQL（不修改数据）

## 1) 单车同车系候选（严格只读）

```sql
SELECT
  c.id,
  c.brand,
  c.model,
  c.model_year,
  c.manufacturer_name,
  c.vehicle_type,
  c.fuel_type,
  c.curb_weight,
  c.wheelbase_mm
FROM vehicle_model t
JOIN vehicle_model c
  ON c.id <> t.id
 AND LOWER(c.vehicle_type) = LOWER(t.vehicle_type)
 AND LOWER(c.fuel_type) = LOWER(t.fuel_type)
 AND c.model_year BETWEEN t.model_year - 4 AND t.model_year + 4
 AND (
      t.manufacturer_name IS NULL
      OR TRIM(t.manufacturer_name) = ''
      OR LOWER(COALESCE(c.manufacturer_name, '')) = LOWER(t.manufacturer_name)
 )
WHERE t.id = :targetVehicleId
ORDER BY c.model_year DESC, c.id DESC
LIMIT 200;
```

## 2) 数据质量检查（只读）

```sql
SELECT
  COUNT(*) AS total,
  SUM(CASE WHEN manufacturer_name IS NULL OR TRIM(manufacturer_name) = '' THEN 1 ELSE 0 END) AS manufacturer_missing,
  SUM(CASE WHEN trademark IS NULL OR TRIM(trademark) = '' THEN 1 ELSE 0 END) AS trademark_missing,
  SUM(CASE WHEN model IS NULL OR TRIM(model) = '' THEN 1 ELSE 0 END) AS model_missing
FROM vehicle_model;
```

## 3) 候选规模评估（只读）

```sql
SELECT
  t.id AS target_id,
  COUNT(c.id) AS candidate_count
FROM vehicle_model t
LEFT JOIN vehicle_model c
  ON c.id <> t.id
 AND LOWER(c.vehicle_type) = LOWER(t.vehicle_type)
 AND LOWER(c.fuel_type) = LOWER(t.fuel_type)
 AND c.model_year BETWEEN t.model_year - 4 AND t.model_year + 4
 AND (
      t.manufacturer_name IS NULL
      OR TRIM(t.manufacturer_name) = ''
      OR LOWER(COALESCE(c.manufacturer_name, '')) = LOWER(t.manufacturer_name)
 )
GROUP BY t.id
ORDER BY candidate_count DESC
LIMIT 100;
```
