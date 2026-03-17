# 2026-03-18 回收价与行情价解耦

## 背景
- 将“行情价”（爬虫）与“回收价”（Excel 导入）解耦。
- `material_price` 表新增 `price_category` 字段。
- 估值逻辑调整为优先使用 `RECYCLE` 类型的价格。

## 变更内容
- 表 `material_price`：
    - 新增 `price_category` (VARCHAR, DEFAULT 'MARKET')
    - 索引调整：`uk_material_price_type_date` -> `uk_material_price_type_date_category`

## 预检查 (Pre-check)
```sql
SELECT COUNT(*) FROM material_price;
SHOW CREATE TABLE material_price;
```

## 执行 (Execute)
执行 `migrate.sql`

## 验证 (Verify)
```sql
-- 验证列存在且默认值为 MARKET
SELECT price_category, COUNT(*) FROM material_price GROUP BY price_category;
-- 预期结果: 
-- MARKET | (所有现有行数)

-- 验证索引
SHOW INDEX FROM material_price;
-- 预期结果: 包含 uk_material_price_type_date_category
```

## 回滚 (Rollback)
```sql
-- 如果需要回滚，删除列并恢复索引
ALTER TABLE material_price DROP INDEX uk_material_price_type_date_category;
ALTER TABLE material_price DROP COLUMN price_category;
CREATE UNIQUE INDEX uk_material_price_type_date ON material_price (type, effective_date);
```
