from __future__ import annotations

import json
import socket
import time
from typing import Any

import requests

from .backend_api import (
    claim_miit_cp_job,
    complete_miit_cp_job,
    fail_miit_cp_job,
    list_pending_miit_cp_jobs,
    update_miit_cp_job_progress,
)
from .cp_sync import sync_cp
from .logging_utils import log


def run_worker(backend: str, token: str, poll_interval_sec: float = 3.0) -> None:
    session = requests.Session()
    if token:
        session.headers.update({"Authorization": f"Bearer {token}"})

    worker_id = socket.gethostname()
    log("miit.worker.start", backend=backend, worker=worker_id)

    while True:
        try:
            pending = list_pending_miit_cp_jobs(session, backend, limit=10)
            if not pending:
                time.sleep(poll_interval_sec)
                continue

            job = pending[0]
            run_id = str(job.get("runId"))
            claim_miit_cp_job(session, backend, run_id, worker=worker_id)

            details_json = job.get("detailsJson") or "{}"
            details = json.loads(details_json) if isinstance(details_json, str) else {}
            config = (details.get("config") or {}) if isinstance(details, dict) else {}

            pc_from = int(config.get("pcFrom"))
            pc_to = int(config.get("pcTo"))
            qymc = config.get("qymc")
            qymc_list = config.get("qymcList")
            cpsb = config.get("cpsb")
            clxh = config.get("clxh")
            clmc = config.get("clmc")
            cpsb_list = config.get("cpsbList") or []
            page_size = int(config.get("pageSize") or 10)
            limit = config.get("limit")
            headful = bool(config.get("headful", True))
            retry_items = config.get("retryItems")

            def report(progress: dict[str, Any]) -> None:
                msg = progress.get("stage")
                inserted = progress.get("inserted")
                updated = progress.get("updated")
                skipped = progress.get("skipped")
                update_miit_cp_job_progress(
                    session,
                    backend,
                    run_id,
                    inserted=inserted if isinstance(inserted, int) else None,
                    updated=updated if isinstance(updated, int) else None,
                    skipped=skipped if isinstance(skipped, int) else None,
                    message=str(msg) if msg else None,
                    details_json=None,  # Do not upload huge config on every progress tick
                )

            report({"stage": "RUNNING", "worker": worker_id})
            result = sync_cp(
                backend=backend,
                token=token,
                pc_from=pc_from,
                pc_to=pc_to,
                qymc=qymc,
                qymc_list=qymc_list,
                cpsb=cpsb,
                cpsb_list=cpsb_list,
                clxh=clxh,
                clmc=clmc,
                page_size=page_size,
                headful=headful,
                limit=limit,
                on_progress=report,
                retry_items=retry_items,
            )
            payload = {"config": config, "progress": {"stage": "DONE", "result": result}}
            complete_miit_cp_job(
                session,
                backend,
                run_id,
                inserted=int(result.get("inserted", 0)),
                updated=int(result.get("updated", 0)),
                skipped=int(result.get("skipped", 0)),
                message="DONE",
                details_json=json.dumps(payload, ensure_ascii=False),
            )
        except KeyboardInterrupt:
            raise
        except Exception as e:
            try:
                run_id = locals().get("run_id")
                if run_id:
                    fail_miit_cp_job(session, backend, run_id, message=str(e), details_json=None)
            except Exception:
                pass
            time.sleep(poll_interval_sec)

