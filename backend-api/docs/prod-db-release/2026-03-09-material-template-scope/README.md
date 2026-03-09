# 生产数据库发布说明（material_template 作用域升级）

## 目标
- 支持模板按 `VEHICLE`（具体车型）和 `VEHICLE_TYPE`（车型类型）双层作用域。
- 支持 `others_price_per_kg_override` 用于“其余”材料单价覆盖。

## 脚本
- 预检查：`precheck.sql`
- 迁移：`migrate.sql`
- 回滚：`rollback.sql`

## 发布顺序
- 先执行 `precheck.sql`。
- 核对结果后执行 `migrate.sql`。
- 再发布应用版本。
- 发布后执行一次 `precheck.sql` 和业务抽样验证。

## 预检查通过标准
- `duplicated_scope_count = 0`
- `null_scope_value_after_backfill = 0`
- `empty_vehicle_type_count` 允许 >0，但需要先确认这批数据是否可清洗。

## 风险点与处理
- 若 `duplicated_scope_count > 0`，先人工合并重复模板再迁移。
- 若 `scope_value` 存在空值，脚本会跳过 `NOT NULL` 收紧，需要先修复脏数据后再执行：
  `ALTER TABLE material_template MODIFY COLUMN scope_value VARCHAR(64) NOT NULL;`
- 所有 DDL 建议在低峰执行，先备份再变更。

## 回滚原则
- 数据库回滚只在应用未切流或已回退旧版本时执行。
- 回滚前先确认新版本写入的 `scope_type/scope_value` 数据影响范围。

## 后续建议
- 当前项目尚未集成 Flyway/Liquibase，建议下一步把本目录脚本迁移到统一版本化迁移体系，避免手工漂移。
