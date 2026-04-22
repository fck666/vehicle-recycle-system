SET @db_name = DATABASE();

SELECT DATABASE() AS current_db, VERSION() AS mysql_version;

SELECT
  table_name,
  column_name,
  generation_expression
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = 'vehicle_model'
  AND column_name IN ('vehicle_type_ci', 'fuel_type_ci', 'manufacturer_name_ci')
ORDER BY column_name;

SELECT
  table_name,
  index_name,
  GROUP_CONCAT(column_name ORDER BY seq_in_index) AS indexed_columns
FROM information_schema.statistics
WHERE table_schema = @db_name
  AND (
    (table_name = 'vehicle_model' AND index_name IN ('idx_vm_same_series_ci_mfr', 'idx_vm_same_series_ci'))
    OR (table_name = 'material_price' AND index_name IN ('idx_material_price_category_type_date'))
    OR (table_name = 'valuation_record' AND index_name IN ('idx_valuation_record_vehicle_time'))
  )
GROUP BY table_name, index_name
ORDER BY table_name, index_name;

SELECT
  (SELECT COUNT(*) FROM vehicle_model) AS vehicle_model_rows,
  (SELECT COUNT(*) FROM material_price) AS material_price_rows,
  (SELECT COUNT(*) FROM valuation_record) AS valuation_record_rows;
