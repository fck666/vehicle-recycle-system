ALTER TABLE material_template_item
  MODIFY COLUMN ratio DECIMAL(5,4) NULL,
  ADD COLUMN pricing_mode VARCHAR(16) NOT NULL DEFAULT 'WEIGHT' AFTER ratio,
  ADD COLUMN fixed_total_price DECIMAL(10,2) NULL AFTER pricing_mode;

UPDATE material_template_item
SET pricing_mode = 'WEIGHT'
WHERE pricing_mode IS NULL OR pricing_mode = '';
