SET @db_name = DATABASE();

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'valuation_record'
        AND column_name = 'details_json'
    ),
    'SELECT 1',
    'ALTER TABLE valuation_record ADD COLUMN details_json LONGTEXT'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure vehicle_type is nullable as we are moving to scope_type
ALTER TABLE material_template MODIFY COLUMN vehicle_type VARCHAR(32) NULL;

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'material_template'
        AND column_name = 'scope_type'
    ),
    'SELECT 1',
    'ALTER TABLE material_template ADD COLUMN scope_type VARCHAR(16) NOT NULL DEFAULT ''VEHICLE_TYPE'''
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
    'SELECT 1',
    'ALTER TABLE material_template ADD COLUMN scope_value VARCHAR(64) NULL'
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
    'SELECT 1',
    'ALTER TABLE material_template ADD COLUMN others_price_per_kg_override DECIMAL(10,2) NULL'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE material_template
SET scope_type = 'VEHICLE_TYPE',
    scope_value = vehicle_type
WHERE (scope_type IS NULL OR TRIM(scope_type) = '')
  AND vehicle_type IS NOT NULL
  AND TRIM(vehicle_type) <> '';

SET @sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM material_template
      WHERE scope_value IS NULL OR TRIM(scope_value) = ''
    ),
    'SELECT ''skip_not_null_scope_value_due_to_dirty_data''',
    'ALTER TABLE material_template MODIFY COLUMN scope_value VARCHAR(64) NOT NULL'
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
        AND table_name = 'material_template'
        AND index_name = 'uk_material_template_scope'
    ),
    'SELECT 1',
    'ALTER TABLE material_template ADD UNIQUE KEY uk_material_template_scope (scope_type, scope_value)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS vehicle_dismantle_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_id BIGINT NOT NULL,
  steel_weight DECIMAL(10,2),
  aluminum_weight DECIMAL(10,2),
  copper_weight DECIMAL(10,2),
  battery_weight DECIMAL(10,2),
  other_weight DECIMAL(10,2),
  details_json LONGTEXT,
  operator_name VARCHAR(64),
  operator_id VARCHAR(64),
  images_json LONGTEXT,
  remark VARCHAR(512),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_vdr_vehicle_id (vehicle_id),
  INDEX idx_vdr_operator (operator_id)
);
