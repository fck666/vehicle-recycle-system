CREATE TABLE IF NOT EXISTS vehicle_model (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  brand VARCHAR(64) NOT NULL,
  model VARCHAR(64) NOT NULL,
  model_year INT NOT NULL,
  fuel_type VARCHAR(32) NOT NULL,
  product_no VARCHAR(64),
  product_id VARCHAR(64),
  product_model VARCHAR(255),
  manufacturer_name VARCHAR(255),
  trademark VARCHAR(128),
  production_address VARCHAR(255),
  registration_address VARCHAR(255),
  release_date DATE,
  effective_date DATE,
  batch_no INT,
  catalog_index INT,
  length_mm INT,
  width_mm INT,
  height_mm INT,
  wheelbase_mm INT,
  front_overhang_mm INT,
  rear_overhang_mm INT,
  approach_angle_deg DECIMAL(6,2),
  departure_angle_deg DECIMAL(6,2),
  front_track_mm INT,
  rear_track_mm INT,
  axle_count INT,
  axle_load_kg VARCHAR(64),
  tire_count INT,
  tire_spec VARCHAR(255),
  steering_type VARCHAR(64),
  has_abs BOOLEAN,
  max_speed_kmh INT,
  curb_weight DECIMAL(10,2) NOT NULL,
  gross_weight DECIMAL(10,2),
  battery_kwh DECIMAL(10,2),
  motor_model VARCHAR(128),
  motor_manufacturer VARCHAR(255),
  displacement_ml INT,
  power_kw DECIMAL(10,2),
  vin_pattern VARCHAR(64),
  chassis_model VARCHAR(128),
  spec_raw_json LONGTEXT,
  vehicle_type VARCHAR(32) NOT NULL,
  source_site VARCHAR(32),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_vm_brand (brand),
  INDEX idx_vm_model (model),
  INDEX idx_vm_product_id (product_id),
  INDEX idx_vm_product_no (product_no),
  INDEX idx_vm_release_date (release_date)
);

CREATE TABLE IF NOT EXISTS material_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_type VARCHAR(32),
  scope_type VARCHAR(16) NOT NULL DEFAULT 'VEHICLE_TYPE',
  scope_value VARCHAR(64) NOT NULL,
  steel_ratio DECIMAL(5,4) NOT NULL,
  aluminum_ratio DECIMAL(5,4) NOT NULL,
  copper_ratio DECIMAL(5,4) NOT NULL,
  recovery_ratio DECIMAL(5,4) NOT NULL,
  others_price_per_kg_override DECIMAL(10,2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_template_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_id BIGINT NOT NULL,
  material_type VARCHAR(32) NOT NULL,
  ratio DECIMAL(5,4) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_material_template_item (template_id, material_type),
  INDEX idx_material_template_item_tid (template_id),
  FOREIGN KEY (template_id) REFERENCES material_template(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS material_price (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(32) NOT NULL,
  price_per_kg DECIMAL(10,2) NOT NULL,
  currency VARCHAR(8),
  unit VARCHAR(16),
  effective_date DATE NOT NULL,
  fetched_at TIMESTAMP NULL,
  source_name VARCHAR(64),
  source_url VARCHAR(512),
  raw_payload LONGTEXT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_material_price_type_date (type, effective_date),
  INDEX idx_material_price_type_date (type, effective_date)
);

CREATE TABLE IF NOT EXISTS material_source_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(32) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  source_name VARCHAR(64) NOT NULL,
  source_url VARCHAR(512) NOT NULL,
  parse_keyword VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_material_source_type (type),
  INDEX idx_material_source_enabled (enabled)
);

CREATE TABLE IF NOT EXISTS valuation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_id BIGINT NOT NULL,
  valuation_result DECIMAL(12,2) NOT NULL,
  steel_value DECIMAL(12,2) NOT NULL,
  aluminum_value DECIMAL(12,2) NOT NULL,
  copper_value DECIMAL(12,2) NOT NULL,
  battery_value DECIMAL(12,2) NOT NULL,
  details_json LONGTEXT,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehicle_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_id BIGINT NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  image_name VARCHAR(128),
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_vehicle_id (vehicle_id)
);

CREATE TABLE IF NOT EXISTS external_vehicle_trim (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source VARCHAR(32) NOT NULL,
  source_trim_id VARCHAR(64) NOT NULL,
  brand VARCHAR(64) NOT NULL,
  series_name VARCHAR(128),
  market_name VARCHAR(255),
  model_year INT,
  energy_type VARCHAR(32),
  official_price DECIMAL(10,2),
  battery_kwh DECIMAL(10,2),
  displacement_ml INT,
  power_kw DECIMAL(10,2),
  curb_weight DECIMAL(10,2),
  cover_url VARCHAR(512),
  page_url VARCHAR(512),
  raw_json LONGTEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ext_trim_source_id (source, source_trim_id),
  INDEX idx_ext_trim_source_id (source, source_trim_id),
  INDEX idx_ext_trim_brand_series_year (brand, series_name, model_year)
);

CREATE TABLE IF NOT EXISTS vehicle_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  miit_vehicle_id BIGINT NOT NULL,
  external_trim_id BIGINT,
  status VARCHAR(16) NOT NULL,
  score DOUBLE,
  matched_by VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_vehicle_mapping_miit (miit_vehicle_id),
  INDEX idx_vehicle_mapping_status (status),
  INDEX idx_vehicle_mapping_ext_id (external_trim_id),
  FOREIGN KEY (external_trim_id) REFERENCES external_vehicle_trim(id)
);

CREATE TABLE IF NOT EXISTS vehicle_mapping_candidate (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  miit_vehicle_id BIGINT NOT NULL,
  external_trim_id BIGINT NOT NULL,
  score DOUBLE NOT NULL,
  rank_no INT NOT NULL,
  matched_by VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_vmc_miit_rank (miit_vehicle_id, rank_no),
  INDEX idx_vmc_miit_rank (miit_vehicle_id, rank_no),
  INDEX idx_vmc_ext (external_trim_id),
  FOREIGN KEY (external_trim_id) REFERENCES external_vehicle_trim(id)
);

CREATE TABLE IF NOT EXISTS vehicle_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_id BIGINT NOT NULL,
  doc_type VARCHAR(32),
  doc_name VARCHAR(255),
  doc_url VARCHAR(512) NOT NULL,
  sha256 VARCHAR(64),
  source_url VARCHAR(512),
  fetched_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_vehicle_doc_vehicle_id (vehicle_id)
);

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) UNIQUE,
  password_hash VARCHAR(255),
  wx_openid VARCHAR(64) UNIQUE,
  wx_unionid VARCHAR(64) UNIQUE,
  phone VARCHAR(32) UNIQUE,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_username (username),
  INDEX idx_user_wx_openid (wx_openid),
  INDEX idx_user_wx_unionid (wx_unionid),
  INDEX idx_user_phone (phone)
);

CREATE TABLE IF NOT EXISTS role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  INDEX idx_role_code (code)
);

CREATE TABLE IF NOT EXISTS user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id),
  INDEX idx_user_role_user_id (user_id),
  INDEX idx_user_role_role_id (role_id)
);

CREATE TABLE IF NOT EXISTS user_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  client_type VARCHAR(16) NOT NULL,
  session_id VARCHAR(64) NOT NULL UNIQUE,
  token_id VARCHAR(64) NOT NULL,
  issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NULL,
  UNIQUE KEY uk_user_session_user_client (user_id, client_type),
  INDEX idx_user_session_user_client (user_id, client_type),
  INDEX idx_user_session_session_id (session_id)
);

CREATE TABLE IF NOT EXISTS job_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(64) NOT NULL UNIQUE,
  job_type VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  actor_user_id BIGINT NULL,
  actor_name VARCHAR(64),
  inserted_count INT,
  updated_count INT,
  skipped_count INT,
  message VARCHAR(512),
  details_json LONGTEXT,
  INDEX idx_job_run_type_time (job_type, started_at),
  INDEX idx_job_run_run_id (run_id)
);

ALTER TABLE valuation_record ADD COLUMN details_json LONGTEXT;
ALTER TABLE material_template MODIFY COLUMN vehicle_type VARCHAR(32) NULL;
ALTER TABLE material_template ADD COLUMN scope_type VARCHAR(16) NOT NULL DEFAULT 'VEHICLE_TYPE';
ALTER TABLE material_template ADD COLUMN scope_value VARCHAR(64) NULL;
ALTER TABLE material_template ADD COLUMN others_price_per_kg_override DECIMAL(10,2) NULL;
UPDATE material_template
SET scope_type = 'VEHICLE_TYPE',
    scope_value = vehicle_type
WHERE (scope_type IS NULL OR scope_type = '')
  AND vehicle_type IS NOT NULL;
ALTER TABLE material_template MODIFY COLUMN scope_value VARCHAR(64) NOT NULL;
ALTER TABLE material_template ADD UNIQUE KEY uk_material_template_scope (scope_type, scope_value);

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
