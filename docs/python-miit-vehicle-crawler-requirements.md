# Python 工信部车型参数爬取（PDF）需求文档

## 1. 背景与目标

系统需要批量获取“工信部机动车产品公告/目录”中可查询到的车型参数，并将 PDF 中的信息结构化入库，用于：
- 建立车型基础库（估值、拆解、对账的统一参照）
- 可追溯与可复核（保留原始 PDF 以便后续重新解析/人工核对）
- 后续扩展（不同批次、不同版本车型参数变化）

核心目标：
- 支持批量采集多个车型（按目录批次、企业、品牌、车型关键字等条件）
- 将 PDF 中字段尽可能准确、完整地入库
- 对“字段缺失/格式不同/扫描版 PDF”具备容错能力
- 可在存在验证码/反爬措施的情况下，提供可运维、可持续的采集方案（不要求完全无人值守，但要能规模化）

## 2. 合规与反爬约束

已知约束：打开每个 PDF 需要完成拖动图片验证码（滑块/拼图类），属于强交互验证。

需求原则（推荐）：
- 优先采用合规方式：对接官方开放接口、购买授权数据、或通过合作渠道获取批量数据
- 若必须自采：采用“人机协同（human-in-the-loop）”采集策略，把验证码步骤显式纳入流程与运维设计，而不是试图绕过

本需求文档不包含规避验证码的细节实现，只定义“可插拔验证码处理接口”和人机协作流程，便于后续在合法合规前提下落地。

## 3. 数据获取范围

### 3.1 采集对象
- 车型“产品技术参数”PDF（见样例图）
- 相关元信息：
  - PDF 下载来源 URL
  - 抓取时间 fetched_at
  - 目录批次/目录序号（如可从页面或 PDF 解析）
  - 解析版本 parser_version

### 3.2 采集规模
- 支持一次采集 N 个车型（N 可配置，默认 200）
- 支持断点续跑：已成功下载/入库的车型可跳过

## 4. 采集流程（Pipeline）

### 4.1 任务输入（Input）
支持以下输入方式：
- 按目录批次/发布日期范围：`batch=320` 或 `release_date=[2020-01-01,2020-12-31]`
- 按企业：`manufacturer contains "江淮"`
- 按品牌/商标：`trademark contains "蔚来"`
- 按产品号/产品ID 精确匹配：`product_no=...` / `product_id=...`
- 按关键词（产品型号/名称）：`keyword="纯电动多用途乘用车"`
- 直接提供待采集 PDF 的 URL 列表（用于外部系统导出后喂给爬虫）

### 4.2 发现阶段（Discover）
输出待处理的“车型候选列表”，每条包含：
- `product_no`、`product_id`（若页面可获取）
- `pdf_page_url`（点击 PDF 的入口）
- `pdf_download_url`（若可直接得到，否则留空）
- `catalog_batch`、`catalog_index`（若页面可获取）

### 4.3 下载阶段（Download）
要求：
- 使用“浏览器会话”下载（支持保持登录态/会话 cookie）
- 支持人机协同验证码处理：当检测到验证码时，任务进入 `WAITING_FOR_CAPTCHA` 状态，等待人工完成后继续
- 每个 PDF 保存为“原始归档文件”，并计算 `sha256` 去重

下载产物：
- 本地临时文件：`./data/pdfs/<date>/<sha256>.pdf`
- 云端归档（推荐 OSS）：`oss://<bucket>/miit/pdfs/<yyyy>/<mm>/<sha256>.pdf`

### 4.4 解析阶段（Parse）
PDF 解析分两类：

**A. 可复制文本的 PDF（文本层存在）**
- 优先使用文本解析（如 pdfplumber/pdfminer 提取）
- 通过字段模板 + 正则 + 版面定位提取关键字段

**B. 扫描版/图片型 PDF（文本层缺失）**
- 走 OCR（推荐 PaddleOCR）
- 结合版面规则（标题区、参数区）做字段定位

解析输出：
- `spec`：结构化字段（见第 6 节字段映射）
- `raw_kv`：原始键值对（尽可能保留）
- `confidence`：字段级置信度（0~1）
- `issues`：解析异常列表（缺失字段、单位异常、疑似识别错误等）

### 4.5 规范化阶段（Normalize）
将解析结果统一为标准单位/格式：
- 尺寸：mm（int）
- 质量：kg（decimal）
- 角度：deg（decimal）
- 功率：kW（decimal）
- 日期：YYYY-MM-DD

对多值字段统一表达：
- 轴荷：`"1269/1551"`（或拆分为数组 `[{axle:1, load:1269}, ...]` 写入 raw_json）
- 轮胎规格：保留原始字符串，必要时再拆解

### 4.6 入库阶段（Upsert）
要求：
- 以 `product_id` 或 `product_no` 作为主键候选（优先 `product_id`），实现幂等 upsert
- 若同一 `product_id` 的 PDF hash 变化，则认为规格可能更新：
  - 更新车型表字段
  - 追加一条 `vehicle_document`（保留历史 PDF）

### 4.7 审核与回填（可选但强烈推荐）
因为 PDF 字段存在缺失与识别误差，建议提供人工复核机制：
- 输出 `review.csv` 或 `review.jsonl`：列出低置信度字段与原始截图坐标/文本片段
- 支持人工修改后回填入库（通过一个“修正文件”或后台接口）

## 5. 验证码处理（人机协同要求）

### 5.1 状态机
- `PENDING`：待处理
- `DISCOVERED`：已发现 PDF 入口
- `WAITING_FOR_CAPTCHA`：等待人工完成验证码
- `DOWNLOADED`：PDF 下载完成
- `PARSED`：解析完成
- `UPSERTED`：已入库
- `FAILED`：失败（带失败原因与可重试标记）

### 5.2 人工介入方式（建议）
- 方案 1：本地有界面浏览器（Playwright/Selenium headful）弹出验证码页面，由操作员完成拖动后继续批量下载
- 方案 2：远程浏览器（ECS + VNC/堡垒机），集中由少量操作员处理验证码，爬虫保持会话复用

要求：
- 会话可持久化（cookie/本地存储），减少验证码触发频率
- 任务队列可暂停/继续，不因等待验证码而丢任务

## 6. 字段映射（样例图对应）

样例图中常见字段（不同 PDF 可能缺失）：
- 产品号：`product_no`（如 ZP7C066301H）
- 产品ID：`product_id`（如 W0147308）
- 产品型号/名称：`product_model`（如 HFC6483ECEV1-W型纯电动多用途乘用车）
- 企业名称：`manufacturer_name`
- 商品商标：`trademark`
- 生产地址：`production_address`
- 注册地址：`registration_address`
- 发布日期：`release_date`
- 生效日期：`effective_date`
- 批次：`batch_no`
- 目录序号：`catalog_index`

主要技术参数（可选）：
- 外形尺寸（长/宽/高 mm）：`length_mm`/`width_mm`/`height_mm`
- 轴距（mm）：`wheelbase_mm`
- 总质量（kg）：`gross_weight`
- 整备质量（kg）：`curb_weight`
- 前悬/后悬（mm）：`front_overhang_mm`/`rear_overhang_mm`
- 接近角/离去角（°）：`approach_angle_deg`/`departure_angle_deg`
- 前轮距/后轮距（mm）：`front_track_mm`/`rear_track_mm`
- 轴数：`axle_count`
- 轴荷（kg）：`axle_load_kg`（或写入 raw_json）
- 轮胎数：`tire_count`
- 轮胎规格：`tire_spec`
- 转向型式：`steering_type`
- 防抱死系统：`has_abs`
- 最高车速（km/h）：`max_speed_kmh`

动力相关（视燃料类型而定）：
- 燃料种类：现有字段 `fuel_type`
- 发动机/电机型号：`motor_model`
- 生产企业：`motor_manufacturer`
- 排量（ml）：`displacement_ml`
- 功率（kW）：`power_kw`

车架/识别信息（可能脱敏）：
- 车辆识别代号：`vin_pattern`
- 底盘型号：`chassis_model`

所有未能稳定映射的字段，统一落在：
- `spec_raw_json`（保存解析到的键值对与元数据，便于后续补字段）

## 7. PDF 归档要求（强烈建议）

建议每个车型至少保存一份“来源 PDF”：
- 便于未来改进解析器后重新跑解析
- 便于人工核对与审计
- 便于解决字段缺失：以后拿到更完整的 PDF 可覆盖/补全

入库方式：
- PDF 文件存 OSS（或本地存储用于开发）
- 数据库保存元数据（`vehicle_document`）：
  - `doc_type = "MIIT_PDF"`
  - `doc_url`（OSS URL）
  - `sha256`
  - `source_url`
  - `fetched_at`

## 8. 质量与准确性要求

### 8.1 字段级置信度
每个字段输出置信度：
- 文本层直接提取：默认 0.9+
- OCR 提取：根据识别置信度与规则匹配程度综合计算（例如 0.5~0.9）

### 8.2 校验规则
- 尺寸/重量必须为正数
- 总质量 >= 整备质量
- 轮胎数、轴数为整数且在合理范围
- VIN pattern 长度与格式校验（若存在）

### 8.3 低置信度处理
当关键字段（`product_id`、`product_no`、`gross_weight`、`curb_weight`）置信度低于阈值（默认 0.8）：
- 记录到 review 输出
- 入库但标记 `issues`（写入 raw_json）

## 9. 运维与可观测性

必须具备：
- run_id（每次运行唯一）
- 任务进度统计：发现/下载/解析/入库/失败数量
- 失败可重试：网络失败、临时验证码失败、解析失败
- 限速与并发控制：避免对来源站点造成压力

## 10. 对接后端（建议）

落库方式二选一：
- 方式 1：直连数据库（快速落地）
- 方式 2：调用后端写入接口（长期推荐，权限更可控）

若走后端 API，建议新增：
- `POST /api/vehicle-specs/batch`：批量 upsert 车型参数 + 写入 spec_raw_json
- `POST /api/vehicles/{id}/documents`：登记 PDF 元数据（doc_url/sha256/source_url/fetched_at）

