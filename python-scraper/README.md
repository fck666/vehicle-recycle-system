# Python 材料价格爬虫

目标：从公开价格页面抓取材料参考价，换算为 `元/kg`，写入后端 `POST /api/material-prices/batch`，供估值计算使用。

## 快速开始

安装依赖：

```bash
cd python-scraper
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

运行（默认写入本地后端 `http://localhost:8080`）：

```bash
python -m price_crawler.cli run
```

仅打印不写入：

```bash
python -m price_crawler.cli run --dry-run
```

指定后端地址与鉴权：

```bash
BACKEND_BASE_URL=http://localhost:8080 BACKEND_TOKEN=xxx python -m price_crawler.cli run
```

