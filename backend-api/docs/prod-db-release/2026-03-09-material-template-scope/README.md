# 生产数据库发布说明（material_template 作用域升级）

## 目标
- 支持模板按 `VEHICLE`（具体车型）和 `VEHICLE_TYPE`（车型类型）双层作用域。
- 支持 `others_price_per_kg_override` 用于“其余”材料单价覆盖。

## 脚本清单
- 预检查（迁移前）：`precheck.sql` —— 检查旧数据是否有重复或空值风险
- 迁移：`migrate.sql` —— 执行 DDL 和 DML
- 后检查（迁移后）：`postcheck.sql` —— 验证新结构是否创建，数据回填是否完整
- 回滚：`rollback.sql` —— 恢复到旧结构（注意数据丢失风险）

## 发布顺序
1. 备份全库。
2. 执行 `precheck.sql`，确认无 `duplicated_vehicle_type_count > 0`。
3. 执行 `migrate.sql`。
4. 执行 `postcheck.sql`，确认 `new_columns_created` 和 `duplicated_scope_count` 符合预期。
5. 发布后端应用版本。
6. 业务抽样验证。

## 预检查通过标准
- `duplicated_vehicle_type_count = 0`
- `empty_vehicle_type_count` 允许 >0，但要知晓这些行在迁移后 `scope_value` 仍为空，且会被保留（跳过 NOT NULL 约束）。

## 风险点与处理
- 若 `duplicated_vehicle_type_count > 0`，必须在迁移前人工合并重复的 `vehicle_type`，否则唯一索引创建会失败。
- 若 `migrate.sql` 中途失败，请根据报错点决定是继续重试还是先 `rollback.sql`。
- 所有 DDL 建议在低峰执行。

## 回滚原则
- 数据库回滚只在应用未切流或已回退旧版本时执行。
- 回滚将丢失 `scope_type=VEHICLE`（新业务）的数据，需评估影响。
