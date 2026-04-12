from __future__ import annotations

from typing import Any

import requests

from .logging_utils import log


def _request_json(
    session: requests.Session,
    method: str,
    url: str,
    *,
    timeout: int,
    op: str,
    headers: dict[str, str] | None = None,
    json_payload: Any | None = None,
) -> dict[str, Any]:
    try:
        resp = session.request(method=method, url=url, timeout=timeout, headers=headers, json=json_payload)
        resp.raise_for_status()
        return resp.json()
    except requests.exceptions.HTTPError as e:
        status_code = e.response.status_code if e.response is not None else None
        response_text = e.response.text[:1000] if e.response is not None and e.response.text else None
        log("miit.backend.http.error", op=op, method=method, url=url, timeout=timeout, status=status_code, response=response_text)
        if status_code == 400 and json_payload is not None:
            import json
            log("miit.cp.upsert.error.400", payload=json.dumps(json_payload, ensure_ascii=False)[:2000], response=response_text)
        raise
    except requests.exceptions.RequestException as e:
        log("miit.backend.request.error", op=op, method=method, url=url, timeout=timeout, err=str(e))
        raise


def post_json(session: requests.Session, url: str, payload: Any, run_id: str | None = None) -> dict[str, Any]:
    headers: dict[str, str] = {}
    if run_id:
        headers["X-Run-Id"] = run_id

    return _request_json(
        session,
        "POST",
        url,
        timeout=60,
        op="post_json",
        headers=headers if headers else None,
        json_payload=payload,
    )


def upsert_vehicle_specs(session: requests.Session, base_url: str, items: list[dict[str, Any]], run_id: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/vehicle-specs/batch"
    return post_json(session, url, {"items": items}, run_id=run_id)


def upsert_vehicle_documents(session: requests.Session, base_url: str, items: list[dict[str, Any]], run_id: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/api/vehicle-documents/batch"
    return post_json(session, url, {"items": items}, run_id=run_id)


def list_pending_miit_cp_jobs(session: requests.Session, base_url: str, limit: int = 10) -> list[dict[str, Any]]:
    url = base_url.rstrip("/") + f"/api/admin/miit-cp-jobs/pending?limit={int(limit)}"
    return _request_json(session, "GET", url, timeout=30, op="list_pending_miit_cp_jobs")


def claim_miit_cp_job(session: requests.Session, base_url: str, run_id: str, worker: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + f"/api/admin/miit-cp-jobs/{run_id}/claim"
    if worker:
        url += "?worker=" + requests.utils.quote(worker)
    return _request_json(session, "POST", url, timeout=30, op="claim_miit_cp_job")


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
    return _request_json(session, "POST", url, timeout=30, op="update_miit_cp_job_progress", json_payload=payload)


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
    return _request_json(session, "POST", url, timeout=60, op="complete_miit_cp_job", json_payload=payload)


def fail_miit_cp_job(session: requests.Session, base_url: str, run_id: str, message: str, details_json: str | None = None) -> dict[str, Any]:
    url = base_url.rstrip("/") + f"/api/admin/miit-cp-jobs/{run_id}/fail"
    payload: dict[str, Any] = {"message": message, "detailsJson": details_json}
    return _request_json(session, "POST", url, timeout=30, op="fail_miit_cp_job", json_payload=payload)
