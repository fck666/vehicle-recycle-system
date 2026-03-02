本工具用于从“懂车帝”拉取车型版本清单，并写入后端的外部车型库（external_vehicle_trim）。

用法示例：

```bash
cd python-dcd-vehicle-crawler
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

python -m dcd_vehicle_crawler.cli import_series \
  --backend http://localhost:8090 \
  --token <ADMIN_TOKEN> \
  --city 北京 \
  --series-ids 39,40,41
```

说明：
- `series-ids` 来自懂车帝车系页面 URL，例如 `https://www.dongchedi.com/motor/series/39` 的 `39`
- 导入后可在管理端“车型关联”页面批量生成候选并人工确认

