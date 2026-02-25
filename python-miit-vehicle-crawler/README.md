# Python 工信部车型参数爬取（PDF）

目标：从工信部“道路机动车辆生产企业及产品信息查询系统”页面获取车型技术参数 PDF，下载归档、解析字段并通过后端 API 批量入库。

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

### 方式 1：从 PDF URL 列表下载并入库

准备 `urls.txt`（每行一个 PDF 页面或 PDF 下载 URL），然后：

```bash
python -m miit_vehicle_crawler.cli download --urls urls.txt
python -m miit_vehicle_crawler.cli parse --pdf-dir data/pdfs
python -m miit_vehicle_crawler.cli upsert --spec-jsonl data/specs.jsonl --backend http://localhost:8090 --token xxx

提示：后端开启了“按端类型单点登录”，建议用 `clientType=SERVICE` 登录生成 token 供脚本使用，避免挤掉 PC 端登录。
```

### 方式 2：人机协同（打开浏览器自行筛选，再导出候选）

```bash
python -m miit_vehicle_crawler.cli discover --output data/candidates.jsonl
```
