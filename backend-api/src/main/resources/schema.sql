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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_type VARCHAR(32) NOT NULL,
  steel_ratio DECIMAL(5,4) NOT NULL,
  aluminum_ratio DECIMAL(5,4) NOT NULL,
  copper_ratio DECIMAL(5,4) NOT NULL,
  recovery_ratio DECIMAL(5,4) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_price (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(32) NOT NULL,
  price_per_kg DECIMAL(10,2) NOT NULL,
  currency VARCHAR(8),
  unit VARCHAR(16),
  effective_date DATE NULL,
  fetched_at TIMESTAMP NULL,
  source_name VARCHAR(64),
  source_url VARCHAR(512),
  raw_payload LONGTEXT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS valuation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_id BIGINT NOT NULL,
  valuation_result DECIMAL(12,2) NOT NULL,
  steel_value DECIMAL(12,2) NOT NULL,
  aluminum_value DECIMAL(12,2) NOT NULL,
  copper_value DECIMAL(12,2) NOT NULL,
  battery_value DECIMAL(12,2) NOT NULL,
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
