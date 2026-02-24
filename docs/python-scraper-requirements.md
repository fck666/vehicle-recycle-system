# Python 抓取金属/电池价格需求文档

## 1. 背景与目标

系统需要定期获取金属回收价格（钢、铝、铜）以及电池回收参考价，并写入后端数据库，用于车辆残值估算与对账追溯。

目标：
- 自动化获取价格数据（可配置频率：默认每日 1 次）
- 统一数据口径（单位、币种、时间戳、来源）
- 可追溯（保留来源、抓取时间、原始值、换算过程）
- 可扩展（后续新增材料类型或新增数据源无需大改）
- 可运维（日志、告警、失败重试、幂等）

非目标：
- 不在本阶段实现复杂的行情预测模型
- 不在本阶段实现前端管理界面（仅提供数据写入接口或 DB 写入）

## 2. 数据范围

### 2.1 材料类型（Material Type）
默认支持：
- steel（钢）
- aluminum（铝）
- copper（铜）
- battery（电池）
- plastic（塑料）
- rubber（橡胶）

可扩展：glass / nickel / lithium / paper 等。

### 2.2 单位与币种（Unit / Currency）
现有后端表字段为 `material_price.price_per_kg`，但电池通常以 `price_per_kwh` 表达。

本需求建议两种落地方式（二选一）：

**A. 最小改动（兼容现有结构）**
- steel/aluminum/copper：写入 `price_per_kg`，单位视为 RMB/kg
- battery：仍写入 `price_per_kg` 字段，但业务语义视为 RMB/kWh（需在抓取模块和后端估值逻辑中保持一致）

**B. 推荐改造（长期更稳）**
- 将 `material_price` 结构升级为：
  - `price`（DECIMAL）
  - `unit`（枚举：KG / KWH / TON 等）
  - `currency`（CNY / USD）
  - `effective_date`（价格生效日期，便于按天取价）
  - `source`（数据源）
  - `raw_payload`（原始抓取片段，JSON，可选）
- 新增 `material_price_history` 用于历史留存，避免 `material_price` 只保留最后一次更新导致不可追溯

本需求文档后续均以“推荐改造”描述口径，但实现时可按 A 快速落地，后续再升级到 B。

## 3. 数据源需求

### 3.1 数据源策略
- 采用“多数据源适配器”模式：每种材料支持多个来源
- 优先级可配置：source_priority = [primary, secondary, ...]
- 若主源失败或数据异常，自动回退到次源

### 3.2 数据质量校验
抓取到的每条价格必须通过以下校验：
- 非空、可解析为数值
- 大于 0
- 波动阈值：相对上一期价格涨跌幅超过阈值（默认 30%）标记为 `SUSPECT`，不自动入库（或进入待确认队列）
- 单位、币种可确定，否则标记为 `INVALID`

### 3.3 来源字段
每条记录需写入：
- `source_name`（例如：SMM / LME / 自定义网站）
- `source_url`（抓取 URL）
- `fetched_at`（抓取时间，UTC+8 也可，但必须统一）
- `effective_date`（价格日期：通常为交易日/发布日）

## 4. 抓取模块功能需求

### 4.1 CLI 运行模式（本地/开发）
提供命令行入口：
- `python -m price_crawler fetch --date 2026-02-24`
- `python -m price_crawler fetch --material steel,copper`
- `python -m price_crawler fetch --dry-run`（只输出不入库）

返回码：
- 0：全部成功
- 2：部分成功（有材料失败）
- 1：全部失败或运行异常

### 4.2 定时运行模式（生产/云）
支持通过以下方式调度（二选一）：
- Linux cron（ECS 上）
- 阿里云函数计算 FC + 定时触发器（推荐）

要求：
- 失败重试：指数退避（例如 1m/5m/15m），最多 3 次
- 告警：连续失败 N 次（默认 3）触发通知（钉钉/邮件/短信，至少预留 webhook）

### 4.3 幂等性与去重
同一材料、同一 `effective_date`、同一 `source` 的数据重复抓取时：
- 若价格相同：忽略写入（no-op）
- 若价格不同：写入历史表并更新“最新表”（或覆盖最新表并记录变更原因）

建议唯一约束：
- `UNIQUE(material_type, effective_date, source)`

### 4.4 统一换算（单位/币种）
若来源给出：
- USD/ton：需换算为 CNY/kg
- USD/lb：需换算为 CNY/kg
- CNY/ton：换算为 CNY/kg

换算要求：
- 汇率来源可配置（固定汇率或额外抓取）
- 记录换算过程：原始值、原始单位、汇率、换算后值

### 4.5 输出与落库方式
建议两种方式（二选一）：

**方式 1（推荐）：通过后端 API 写入**
- Python 仅持有 API Token（或内网访问控制），不直接持有 DB 密码
- 后端新增/使用接口：
  - `POST /api/material-prices/batch`：批量 upsert 最新价格 + 追加历史
- 优点：安全、易演进

**方式 2：直连数据库写入**
- Python 使用 MySQL 连接信息（建议只写权限账号）
- 优点：不依赖后端接口
- 缺点：泄露风险、耦合 DB 结构

本阶段若后端尚无写入 API，可先采用方式 2 以快速上线；上线后优先切换方式 1。

## 5. 目录结构与工程规范（建议）

建议新建目录：
- `python-scraper/`
  - `price_crawler/`
    - `sources/`（各数据源适配器）
    - `normalizers/`（单位、币种换算）
    - `storage/`（API 写入或 DB 写入实现）
    - `models.py`
    - `cli.py`
  - `pyproject.toml`（或 requirements.txt）
  - `README.md`

日志：
- 标准输出为 JSON 或 key=value，便于云日志采集
- 每次运行打印 run_id，方便追踪一整次任务

配置：
- 只从环境变量读取密钥/密码
- 支持 `.env`（开发）但不提交到 git

## 6. 验收标准（Acceptance Criteria）

必须满足：
- 能抓取并写入 steel/aluminum/copper/battery/plastic/rubber 价格（至少 1 个可用来源）
- 运行时失败重试与部分成功返回码生效
- 记录 source、fetched_at、effective_date
- 单位/币种换算可用（至少覆盖 ton→kg）
- 可在后端估值中读取到最新价格并参与计算

建议满足：
- 多数据源回退
- 波动阈值检测与可疑数据拦截
- 历史留存与可追溯

## 7. 与后端的接口契约（草案）

若采用“通过后端 API 写入”，建议新增：

### 7.1 批量写入最新价
- Method: `POST /api/material-prices/batch`
- Request JSON:
  - `items`: array
    - `materialType`: string
    - `price`: number
    - `unit`: string
    - `currency`: string
    - `effectiveDate`: string (YYYY-MM-DD)
    - `sourceName`: string
    - `sourceUrl`: string
    - `fetchedAt`: string (ISO8601)
    - `rawPayload`: object (optional)
- Response:
  - `inserted`: int
  - `updated`: int
  - `skipped`: int

鉴权：
- 生产环境必须有（例如 Bearer Token）
- 开发环境可关闭但必须可配置
