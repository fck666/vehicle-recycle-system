from __future__ import annotations

from typing import Any

import requests

from .logging_utils import log


def post_json(session: requests.Session, url: str, payload: Any, run_id: str | None = None) -> dict[str, Any]:
    headers: dict[str, str] = {}
    if run_id:
        headers["X-Run-Id"] = run_id

    # Debug: log payload for 400 errors
    try:
        resp = session.post(url, json=payload, timeout=60, headers=headers if headers else None)
        resp.raise_for_status()
    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 400:
            # Log the payload that caused 400
            import json
            log("miit.cp.upsert.error.400", payload=json.dumps(payload, ensure_ascii=False)[:2000], response=e.response.text)
        raise e
    return resp.json()


def upsert_vehicle_specs(session: requests.Session, base_url: str, items: list[dict[str, Any]], run_id: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/vehicle-specs/batch"
    return post_json(session, url, {"items": items}, run_id=run_id)


def upsert_vehicle_documents(session: requests.Session, base_url: str, items: list[dict[str, Any]], run_id: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/vehicle-documents/batch"
    return post_json(session, url, {"items": items}, run_id=run_id)


def list_pending_miit_cp_jobs(session: requests.Session, base_url: str, limit: int = 10) -> list[dict[str, Any]]:
    url = base_url.rstrip("/") + f"/api/admin/miit-cp-jobs/pending?limit={int(limit)}"
    resp = session.get(url, timeout=30)
    resp.raise_for_status()
    return resp.json()


def claim_miit_cp_job(session: requests.Session, base_url: str, run_id: str, worker: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + f"/api/admin/miit-cp-jobs/{run_id}/claim"
    if worker:
        url += "?worker=" + requests.utils.quote(worker)
    resp = session.post(url, timeout=30)
    resp.raise_for_status()
    return resp.json()


def update_miit_cp_job_progress(
    session: requests.Session,
    base_url: str,
    run_id: str,
    inserted: int | None = None,
    updated: int | None = None,
    skipped: int | None = None,
    message: str | None = None,
    details_json: str | None = None,
) -> dict[str, Any]:
    url = base_url.rstrip("/") + f"/api/admin/miit-cp-jobs/{run_id}/progress"
    payload: dict[str, Any] = {
        "inserted": inserted,
        "updated": updated,
        "skipped": skipped,
        "message": message,
        "detailsJson": details_json,
    }
    resp = session.post(url, json=payload, timeout=30)
    resp.raise_for_status()
    return resp.json()


def complete_miit_cp_job(
    session: requests.Session,
    base_url: str,
    run_id: str,
    inserted: int | None = None,
    updated: int | None = None,
    skipped: int | None = None,
    message: str | None = None,
    details_json: str | None = None,
) -> dict[str, Any]:
    url = base_url.rstrip("/") + f"/api/admin/miit-cp-jobs/{run_id}/complete"
    payload: dict[str, Any] = {
        "inserted": inserted,
        "updated": updated,
        "skipped": skipped,
        "message": message,
        "detailsJson": details_json,
    }
    resp = session.post(url, json=payload, timeout=60)
    resp.raise_for_status()
    return resp.json()


def fail_miit_cp_job(session: requests.Session, base_url: str, run_id: str, message: str, details_json: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + f"/api/admin/miit-cp-jobs/{run_id}/fail"
    payload: dict[str, Any] = {"message": message, "detailsJson": details_json}
    resp = session.post(url, json=payload, timeout=30)
    resp.raise_for_status()
    return resp.json()
