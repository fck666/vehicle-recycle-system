## 变更目标

- 为“同车系候选”和“估值历史/材料价格”补充低风险性能索引。
- 为 `vehicle_model` 增加大小写归一的生成列，支撑同车系接口的索引命中。

## 变更内容

- `vehicle_model`
  - 新增生成列：`vehicle_type_ci`、`fuel_type_ci`、`manufacturer_name_ci`
  - 新增索引：`idx_vm_same_series_ci_mfr`
  - 新增索引：`idx_vm_same_series_ci`
- `material_price`
  - 新增索引：`idx_material_price_category_type_date`
- `valuation_record`
  - 新增索引：`idx_valuation_record_vehicle_time`

## 风险说明

- `vehicle_model` 的新增生成列和索引会触发表结构变更，执行期间可能存在短暂 DDL 锁等待。
- 当前后端代码已内置“索引列不存在时自动回退旧查询”的降级逻辑，因此可以先发代码、后做数据库迁移；迁移完成后自动切到快路径。

## 执行顺序

1. 先执行 `precheck.sql`
2. 执行 `migrate.sql`
3. 执行 `postcheck.sql`
4. 再观察接口耗时日志：
   - `same-series vehicleId=...`
   - `valuation vehicleId=...`
   - `GET /api/vehicles/...`

## 回滚说明

- 回滚会删除新索引和生成列。
- 回滚后接口仍可工作，但“同车系”会退回旧查询路径，性能会回落。
