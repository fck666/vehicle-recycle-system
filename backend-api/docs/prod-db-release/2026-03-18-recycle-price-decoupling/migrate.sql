
-- 1. 新增 price_category 字段，默认值为 'MARKET'
ALTER TABLE material_price 
ADD COLUMN price_category VARCHAR(20) NOT NULL DEFAULT 'MARKET' COMMENT '价格类型：MARKET-行情价，RECYCLE-回收价';

-- 2. 删除旧的唯一索引 (type, effective_date)
DROP INDEX uk_material_price_type_date ON material_price;

-- 3. 创建新的唯一索引 (type, effective_date, price_category)
CREATE UNIQUE INDEX uk_material_price_type_date_category 
ON material_price (type, effective_date, price_category);

-- 4. 验证列添加是否成功
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, IS_NULLABLE
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'material_price' 
AND COLUMN_NAME = 'price_category';
