from __future__ import annotations

import json
import re
from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal

import requests

from ..http_client import HttpConfig, get_text
from ..models import MaterialPriceItem


@dataclass(frozen=True)
class PpiMapping:
    material_type: str
    source_url: str
    source_name: str
    pattern: re.Pattern[str]
    unit: str = "CNY/TON"
    currency: str = "CNY"


MAPPINGS: list[PpiMapping] = [
    PpiMapping(
        material_type="steel",
        source_name="生意社-螺纹钢",
        source_url="https://hrb.100ppi.com/",
        pattern=re.compile(r"螺纹钢参考价为\s*([0-9.]+).*?(\d{4}-\d{2}-\d{2})", re.S),
    ),
    PpiMapping(
        material_type="aluminum",
        source_name="生意社-铝",
        source_url="https://al.100ppi.com/",
        pattern=re.compile(r"铝参考价为\s*([0-9.]+).*?(\d{4}-\d{2}-\d{2})", re.S),
    ),
    PpiMapping(
        material_type="copper",
        source_name="生意社-铜",
        source_url="https://cu.100ppi.com/",
        pattern=re.compile(r"铜参考价为\s*([0-9.]+).*?(\d{4}-\d{2}-\d{2})", re.S),
    ),
    PpiMapping(
        material_type="battery",
        source_name="生意社-碳酸锂(电池级)",
        source_url="https://tsl.100ppi.com/",
        pattern=re.compile(r"碳酸锂-电池级参考价为\s*([0-9.]+).*?(\d{4}-\d{2}-\d{2})", re.S),
    ),
    PpiMapping(
        material_type="plastic",
        source_name="生意社-PP(拉丝)",
        source_url="https://pp.100ppi.com/",
        pattern=re.compile(r"PP\(拉丝\)参考价为\s*([0-9.]+).*?(\d{4}-\d{2}-\d{2})", re.S),
    ),
    PpiMapping(
        material_type="rubber",
        source_name="生意社-天然橡胶",
        source_url="https://nr.100ppi.com/",
        pattern=re.compile(r"天然橡胶参考价为\s*([0-9.]+).*?(\d{4}-\d{2}-\d{2})", re.S),
    ),
]


def fetch_reference_prices(session: requests.Session, cfg: HttpConfig, only_types: set[str] | None = None) -> list[MaterialPriceItem]:
    now = datetime.now()
    items: list[MaterialPriceItem] = []
    for m in MAPPINGS:
        if only_types is not None and m.material_type not in only_types:
            continue
        html = get_text(session, m.source_url, cfg)
        match = m.pattern.search(html)
        if match is None:
            raise RuntimeError(f"no match for {m.material_type} from {m.source_url}")
        source_price = Decimal(match.group(1))
        effective = date.fromisoformat(match.group(2))
        items.append(
            MaterialPriceItem(
                material_type=m.material_type,
                source_name=m.source_name,
                source_url=m.source_url,
                effective_date=effective,
                fetched_at=now,
                source_price=source_price,
                source_unit=m.unit,
                currency=m.currency,
            )
        )
    return items


def build_raw_payload(item: MaterialPriceItem) -> str:
    return json.dumps(
        {
            "sourceName": item.source_name,
            "sourceUrl": item.source_url,
            "sourcePrice": str(item.source_price),
            "sourceUnit": item.source_unit,
            "currency": item.currency,
            "effectiveDate": item.effective_date.isoformat(),
            "fetchedAt": item.fetched_at.isoformat(timespec="seconds"),
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )
