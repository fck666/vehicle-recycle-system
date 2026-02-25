from __future__ import annotations

from typing import Any

import requests

from ..http_client import HttpConfig, post_json
from ..models import MaterialPriceItem
from ..sources.ppi_reference import build_raw_payload


def batch_upsert(
    session: requests.Session,
    cfg: HttpConfig,
    base_url: str,
    token: str | None,
    items: list[MaterialPriceItem],
    run_id: str | None = None,
) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/material-prices/batch"

    payload_items: list[dict[str, Any]] = []
    for item in items:
        payload_items.append(
            {
                "type": item.material_type,
                "pricePerKg": float(item.price_per_kg),
                "currency": item.currency,
                "unit": item.source_unit,
                "effectiveDate": item.effective_date.isoformat(),
                "fetchedAt": item.fetched_at.isoformat(timespec="seconds"),
                "sourceName": item.source_name,
                "sourceUrl": item.source_url,
                "rawPayload": build_raw_payload(item),
            }
        )

    req_body = {"items": payload_items}

    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if run_id:
        headers["X-Run-Id"] = run_id
    if headers:
        session.headers.update(headers)

    resp = post_json(session, url, req_body, cfg)
    return resp.json()
