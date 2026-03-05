# Python 工信部车型参数爬取

目标：从工信部“道路机动车辆生产企业及产品信息查询系统”页面获取车型技术参数，解析字段、下载图片并通过后端 API 批量入库。

来源站点：`https://service.miit-eidc.org.cn/miitxxgk/gonggao/xxgk/index`

## 安装

```bash
cd python-miit-vehicle-crawler
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python -m playwright install chromium
```

## 运行方式

### 方式 1：按批次/品牌抓取“公告产品主要技术参数”网页并入库（推荐）

特点：
- **全自动列表抓取**：走 `doCpQuery` 接口，支持指定批次范围与企业/品牌条件，自动翻页获取全部数据。
- **自动过滑块验证**：详情页访问遇到“拖动滑块拼图”时，脚本会自动使用 OpenCV 识别缺口并模拟鼠标拖动通过验证。若自动识别失败，会提示人工介入。
- **智能防重**：访问详情页前会自动查询后端数据库，若车型已存在（根据 `productId` 和 `productNo`），则跳过详情页抓取，避免重复触发验证和无效请求。
- **完整数据归档**：会将详情页原始 HTML 作为文档（MIIT_HTML）入库，并下载页面图片入库到车型图片（HTML 中的图片链接会被重写为本地/云端地址）。

**单次执行示例：**

```bash
python -m miit_vehicle_crawler.cli cp-sync \
  --pc-from 398 --pc-to 398 \
  --qymc 大众 \
  --backend http://localhost:8090 \
  --token <ADMIN_TOKEN>
```

**按“品牌范围”批量抓取示例（同一批次内依次抓多个产品商标）：**

```bash
python -m miit_vehicle_crawler.cli cp-sync \
  --pc-from 397 --pc-to 398 \
  --cpsb-list "大众牌,奥迪(AUDI)牌" \
  --backend http://localhost:8090 \
  --token <ADMIN_TOKEN>
```

### 方式 2：作为本机 Worker 轮询执行管理端任务（生产环境推荐）

Worker 模式是推荐的运行方式，它会自动监听后端 API 创建的任务并执行抓取。

**1. 启动 Worker：**

```bash
python3 -m miit_vehicle_crawler.cli worker --backend http://localhost:8090 --token "any"
```
（注：后端已放行任务接口，Token 可随意填写或留空）

**2. 在管理端创建任务：**
- 访问管理后台：`http://localhost:5173/miit-cp-jobs`
- 点击“创建任务”
- 填写批次范围（如 397-398）
- 填写企业名称列表（如 `一汽-大众`，支持简称模糊匹配）
- 提交后，Worker 会自动领取任务并开始抓取。

**3. 查看结果：**
- 任务列表页会实时显示进度。
- 抓取完成后，可在“车辆规格管理”页面查看入库的车型数据。
- 点击详情页的“HTML 存档”链接可查看原始网页镜像。

### 方式 3：命令行单次抓取（调试用）

```bash
python -m miit_vehicle_crawler.cli discover --output data/candidates.jsonl
```
