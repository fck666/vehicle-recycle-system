SET @db_name = DATABASE();

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'vehicle_model'
        AND column_name = 'vehicle_type_ci'
    ),
    'SELECT 1',
    'ALTER TABLE vehicle_model ADD COLUMN vehicle_type_ci VARCHAR(32) GENERATED ALWAYS AS (LOWER(COALESCE(vehicle_type, ''''))) STORED'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'vehicle_model'
        AND column_name = 'fuel_type_ci'
    ),
    'SELECT 1',
    'ALTER TABLE vehicle_model ADD COLUMN fuel_type_ci VARCHAR(32) GENERATED ALWAYS AS (LOWER(COALESCE(fuel_type, ''''))) STORED'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'vehicle_model'
        AND column_name = 'manufacturer_name_ci'
    ),
    'SELECT 1',
    'ALTER TABLE vehicle_model ADD COLUMN manufacturer_name_ci VARCHAR(255) GENERATED ALWAYS AS (LOWER(COALESCE(manufacturer_name, ''''))) STORED'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = @db_name
        AND table_name = 'vehicle_model'
        AND index_name = 'idx_vm_same_series_ci_mfr'
    ),
    'SELECT 1',
    'ALTER TABLE vehicle_model ADD INDEX idx_vm_same_series_ci_mfr (vehicle_type_ci, fuel_type_ci, manufacturer_name_ci, model_year, id)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = @db_name
        AND table_name = 'vehicle_model'
        AND index_name = 'idx_vm_same_series_ci'
    ),
    'SELECT 1',
    'ALTER TABLE vehicle_model ADD INDEX idx_vm_same_series_ci (vehicle_type_ci, fuel_type_ci, model_year, id)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = @db_name
        AND table_name = 'material_price'
        AND index_name = 'idx_material_price_category_type_date'
    ),
    'SELECT 1',
    'ALTER TABLE material_price ADD INDEX idx_material_price_category_type_date (price_category, type, effective_date)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = @db_name
        AND table_name = 'valuation_record'
        AND index_name = 'idx_valuation_record_vehicle_time'
    ),
    'SELECT 1',
    'ALTER TABLE valuation_record ADD INDEX idx_valuation_record_vehicle_time (vehicle_id, created_time)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
