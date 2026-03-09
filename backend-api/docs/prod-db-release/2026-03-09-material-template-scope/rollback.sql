SET @db_name = DATABASE();

DROP TABLE IF EXISTS vehicle_dismantle_record;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = @db_name
        AND table_name = 'material_template'
        AND index_name = 'uk_material_template_scope'
    ),
    'ALTER TABLE material_template DROP INDEX uk_material_template_scope',
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
        AND table_name = 'valuation_record'
        AND column_name = 'details_json'
    ),
    'ALTER TABLE valuation_record DROP COLUMN details_json',
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
        AND table_name = 'material_template'
        AND column_name = 'scope_value'
    ),
    'ALTER TABLE material_template DROP COLUMN scope_value',
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
        AND table_name = 'material_template'
        AND column_name = 'scope_type'
    ),
    'ALTER TABLE material_template DROP COLUMN scope_type',
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
        AND table_name = 'material_template'
        AND column_name = 'others_price_per_kg_override'
    ),
    'ALTER TABLE material_template DROP COLUMN others_price_per_kg_override',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
