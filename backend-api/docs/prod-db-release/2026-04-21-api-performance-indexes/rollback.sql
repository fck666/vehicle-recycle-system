SET @db_name = DATABASE();

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = @db_name
        AND table_name = 'vehicle_model'
        AND index_name = 'idx_vm_same_series_ci_mfr'
    ),
    'ALTER TABLE vehicle_model DROP INDEX idx_vm_same_series_ci_mfr',
    'SELECT 1'
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
    'ALTER TABLE vehicle_model DROP INDEX idx_vm_same_series_ci',
    'SELECT 1'
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
    'ALTER TABLE material_price DROP INDEX idx_material_price_category_type_date',
    'SELECT 1'
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
    'ALTER TABLE valuation_record DROP INDEX idx_valuation_record_vehicle_time',
    'SELECT 1'
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
    'ALTER TABLE vehicle_model DROP COLUMN manufacturer_name_ci',
    'SELECT 1'
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
    'ALTER TABLE vehicle_model DROP COLUMN fuel_type_ci',
    'SELECT 1'
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
        AND column_name = 'vehicle_type_ci'
    ),
    'ALTER TABLE vehicle_model DROP COLUMN vehicle_type_ci',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
