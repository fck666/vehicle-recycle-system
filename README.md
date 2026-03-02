# 车辆回收估值系统（Vehicle Recycle System）

本仓库用于构建“车辆回收估值”相关的后端服务与数据采集工具，目标是把车辆基础信息、材料回收单价、车型技术参数（工信部 PDF）统一沉淀到数据库，并提供估值 API 供前端（含后续微信小程序）调用。

## 功能概览

- 车辆基础库：车型基础信息、图片、PDF 文档元数据
- 材料价格库：steel/aluminum/copper/battery/plastic/rubber 最新参考价入库，并保留来源与抓取元数据
- 车型参数入库：支持批量 upsert 车型参数，并把原始解析信息写入 `spec_raw_json`
- 残值估算：按车型材料配比 + 材料单价 + 回收系数计算估值，并记录历史

## 仓库结构

- 后端 API（Spring Boot）：[backend-api](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api)
- 管理后台（Vue）：[admin-web](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/admin-web)
- 材料价格爬虫（生意社）：[python-scraper](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/python-scraper)
- 工信部车型参数 PDF 采集/解析/入库：[python-miit-vehicle-crawler](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/python-miit-vehicle-crawler)
- 需求文档：[docs](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/docs)

## 技术栈

- 后端：Java 17、Spring Boot、Spring Web、Spring Data JPA、MySQL
- Python 工具：
  - 价格爬虫：requests
  - 工信部 PDF：playwright（浏览器会话/人机协同验证码）、pdfplumber（文本层解析）、requests（入库）

## 快速开始（本地开发）

### 1) 准备数据库

- 本地安装 MySQL，并创建数据库 `scrap_system`
- 后端默认使用 `dev,local` profile（见 [application.yaml](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/resources/application.yaml)）

配置数据库连接：
- `backend-api/src/main/resources/application-dev.yaml`：URL/用户名
- `backend-api/src/main/resources/application-local.yaml`：本地密码（可直接写明文供本机使用；也支持用环境变量 `DB_PASSWORD` 覆盖；该文件在根目录 .gitignore 中已忽略）

### 2) 启动后端

进入后端目录启动：

```bash
cd backend-api
./mvnw spring-boot:run
```

默认端口：`http://localhost:8090`

默认会初始化一个管理账号（非 test 环境）：
- 用户名：`fcc`（可通过环境变量 `ADMIN_USERNAME` 覆盖）
- 密码：`12345`（开发/本地默认；生产环境请通过环境变量 `ADMIN_PASSWORD` 显式设置）

JWT 密钥：
- 开发环境默认从 `JWT_SECRET` 读取（未设置会使用开发用默认值）
- 生产环境必须设置 `JWT_SECRET`

开发环境会：
- 自动建表/更新表结构（`ddl-auto=update`）
- 初始化基础数据（非 test 环境）：车辆样例、材料模板、材料价格（见 [DataInitializer.java](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/java/com/scrap_system/backend_api/config/DataInitializer.java)）

### 2.1) 启动管理后台（Vue）

要求：后端先启动在 `http://localhost:8090`。

```bash
cd admin-web
npm install
npm run dev
```

本地访问：`http://localhost:5174/`（开发态已配置 `/api` 代理到后端）。

管理后台能力：
- 车型管理、材料价格、估值模板维护
- 抓取任务记录查看（材料抓取/车型入库）
- 用户管理（仅 ADMIN 可见/可操作）
- 车型关联（方案C）：导入外部车型版本库并半自动关联通用名称（仅 ADMIN）

### 3) 跑材料价格爬虫（生意社）

```bash
cd python-scraper
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

python -m price_crawler.cli run
```

- 默认写入后端：`http://localhost:8090/api/material-prices/batch`
- 支持 `--dry-run` 仅打印不写入
- 可通过环境变量覆盖：
  - `BACKEND_BASE_URL`（默认 http://localhost:8090）
  - `BACKEND_TOKEN`（必填：后端写入接口已开启 JWT 鉴权；建议用 `clientType=SERVICE` 生成 token，避免影响 PC 登录）

材料与数据源映射见：[ppi_reference.py](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/python-scraper/price_crawler/sources/ppi_reference.py)

后端也内置了“每日定时抓取生意社材料价格并入库”的定时任务（用于部署环境免装 Python）：
- 配置项：`app.material-price-fetch.enabled/cron/zone`（见 [application.yaml](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/resources/application.yaml)）
- 默认 cron：每天 02:10（Asia/Shanghai）

### 4) 跑工信部车型参数 PDF（人机协同）

工信部查询入口（来源站点）：

- `https://service.miit-eidc.org.cn/miitxxgk/gonggao/xxgk/index`

安装依赖（首次需安装浏览器）：

```bash
cd python-miit-vehicle-crawler
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python -m playwright install chromium
```

两种常用方式：

1）你已经有 PDF URL 列表（`urls.txt`，每行一个 URL）

```bash
python -m miit_vehicle_crawler.cli download --urls urls.txt
python -m miit_vehicle_crawler.cli parse
python -m miit_vehicle_crawler.cli upsert --backend http://localhost:8090
```

2）人机协同筛选（会打开浏览器，你手动筛选/搜索后回车导出候选链接）

```bash
python -m miit_vehicle_crawler.cli discover --output data/candidates.jsonl
```

说明：
- 下载阶段若检测到验证码，会提示你在浏览器完成验证后继续
- PDF 归档默认落本地 `data/pdfs/<date>/<sha256>.pdf`，并计算 sha256 用于去重

## 后端 API 速览

### 材料价格

- `GET /api/material-prices`：列表
- `GET /api/material-prices/{type}`：按 type 查询
- `POST /api/material-prices`：单条 upsert
- `POST /api/material-prices/batch`：批量 upsert

字段约定（核心字段）：
- `type`：steel/aluminum/copper/battery/plastic/rubber
- `pricePerKg`：统一写入“元/kg”字段；其中 battery 目前在估值中按“元/kWh”使用（见下方估值说明）
- `sourceName/sourceUrl/fetchedAt/effectiveDate/unit/currency/rawPayload`：用于追溯来源与抓取过程

实现参考：[MaterialPriceController.java](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/java/com/scrap_system/backend_api/controller/MaterialPriceController.java)

### 车型参数与文档

- `POST /api/vehicle-specs/batch`：批量 upsert 车型参数（按 productId 优先、否则 productNo）
- `POST /api/vehicle-documents/batch`：批量 upsert 车型 PDF 文档元数据（按 productId/productNo 关联车型，按 sha256/docUrl 去重）
- `POST /api/vehicles/{vehicleId}/documents`：单车添加文档（需已知 vehicleId）

实现参考：
- [VehicleSpecController.java](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/java/com/scrap_system/backend_api/controller/VehicleSpecController.java)
- [VehicleDocumentBatchController.java](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/java/com/scrap_system/backend_api/controller/VehicleDocumentBatchController.java)

### 估值

- `POST /api/valuation/{vehicleId}`：使用车型表参数 + 模板配比估值
- `POST /api/valuation/{vehicleId}/precise`：使用请求体提供的更精确参数估值
- `GET /api/valuation/history?vehicleId=...`：估值历史

估值逻辑（简化）：
- steel/aluminum/copper：按 `整备质量 * 材料比例 * 单价(元/kg)`
- battery：若 `batteryKwh > 0`，按 `batteryKwh * 单价(元/kWh)`
- 总价：`(steelValue + aluminumValue + copperValue + batteryValue) * recoveryRatio`

实现参考：[ValuationService.java](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/java/com/scrap_system/backend_api/service/ValuationService.java)

## 数据表与关键字段

初始化表结构见：[schema.sql](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/resources/schema.sql)

- `vehicle_model`：车型主表（支持 `spec_raw_json` 存放原始解析信息）
- `material_template`：车型材料配比模板（steel/aluminum/copper/recovery）
- `material_price`：材料价格最新表（含来源/日期/原始载荷 raw_payload）
- `valuation_record`：估值结果历史
- `vehicle_image`：车辆图片
- `vehicle_document`：车辆文档（MIIT PDF 等）

## 配置与安全

- 不要把任何数据库密码/Token 写进仓库：使用本地忽略文件或环境变量注入
- 生产环境推荐使用 `prod` profile，通过环境变量提供 DB 连接（见 [application-prod.yaml](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/src/main/resources/application-prod.yaml)）

## 后续：微信小程序对接建议

- 小程序侧主要对接后端接口：车辆列表、车型详情（含文档/图片）、估值、价格展示
- 建议增加的后端能力：
  - 鉴权（小程序登录态/JWT）
  - 车辆搜索与分页
  - 估值参数校验与单位明确化（尤其 battery 单价的单位语义）

## 常见问题（Troubleshooting）

- MySQL 连接失败：检查数据库是否创建、端口/用户名/密码是否正确，以及 `dev,local` profile 是否加载
- 工信部 PDF 下载卡在验证码：属于人机协同设计的一部分；在浏览器完成验证后回到命令行继续
- PDF 解析字段缺失：扫描版 PDF 可能无文本层；当前解析器以文本层为主，OCR 可作为后续增强方向
