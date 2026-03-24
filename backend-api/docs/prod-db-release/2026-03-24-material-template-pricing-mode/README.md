## 变更目标

- 支持材料模板按两种计价模式配置：
  - `WEIGHT`：按重量和单价计算
  - `FIXED_TOTAL`：按固定总价计算

## 数据库变更

- `material_template_item.ratio` 改为可空
- 新增 `material_template_item.pricing_mode`
- 新增 `material_template_item.fixed_total_price`
