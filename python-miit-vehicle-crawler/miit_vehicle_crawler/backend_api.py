from __future__ import annotations

from typing import Any

import requests


def post_json(session: requests.Session, url: str, payload: Any, run_id: str | None = None) -> dict[str, Any]:
    headers: dict[str, str] = {}
    if run_id:
        headers["X-Run-Id"] = run_id
    resp = session.post(url, json=payload, timeout=30, headers=headers if headers else None)
    resp.raise_for_status()
    return resp.json()


def upsert_vehicle_specs(session: requests.Session, base_url: str, items: list[dict[str, Any]], run_id: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/vehicle-specs/batch"
    return post_json(session, url, {"items": items}, run_id=run_id)


def upsert_vehicle_documents(session: requests.Session, base_url: str, items: list[dict[str, Any]], run_id: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/vehicle-documents/batch"
    return post_json(session, url, {"items": items}, run_id=run_id)
