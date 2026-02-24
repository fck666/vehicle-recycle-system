from __future__ import annotations

from typing import Any

import requests


def post_json(session: requests.Session, url: str, payload: Any) -> dict[str, Any]:
    resp = session.post(url, json=payload, timeout=30)
    resp.raise_for_status()
    return resp.json()


def upsert_vehicle_specs(session: requests.Session, base_url: str, items: list[dict[str, Any]]) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/vehicle-specs/batch"
    return post_json(session, url, {"items": items})


def upsert_vehicle_documents(session: requests.Session, base_url: str, items: list[dict[str, Any]]) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/vehicle-documents/batch"
    return post_json(session, url, {"items": items})
