from __future__ import annotations

import argparse
import json
import uuid

from .env import getenv, getenv_int
from .http_client import HttpConfig, new_session
from .logging_utils import log
from .sources.ppi_reference import fetch_reference_prices
from .storage.backend_api import batch_upsert


def main() -> None:
    parser = argparse.ArgumentParser(prog="price-crawler")
    sub = parser.add_subparsers(dest="cmd", required=True)

    run = sub.add_parser("run")
    run.add_argument("--dry-run", action="store_true")
    run.add_argument("--only", default="", help="comma separated: steel,aluminum,copper,battery,plastic,rubber")
    run.add_argument("--backend", default=getenv("BACKEND_BASE_URL", "http://localhost:8090"))

    args = parser.parse_args()

    if args.cmd == "run":
        run_once(args)


def run_once(args: argparse.Namespace) -> None:
    run_id = str(uuid.uuid4())
    timeout = getenv_int("HTTP_TIMEOUT_SECONDS", 20)
    retries = getenv_int("HTTP_MAX_RETRIES", 3)
    cfg = HttpConfig(timeout_seconds=timeout, max_retries=retries)

    token = getenv("BACKEND_TOKEN")
    only_types = None
    if args.only.strip():
        only_types = {t.strip() for t in args.only.split(",") if t.strip()}

    session = new_session()

    log("price_crawler.start", run_id=run_id, backend=args.backend, dry_run=bool(args.dry_run), only=sorted(only_types) if only_types else None)
    items = fetch_reference_prices(session, cfg, only_types=only_types)
    log(
        "price_crawler.fetched",
        run_id=run_id,
        count=len(items),
        items=[
            {
                "type": i.material_type,
                "pricePerKg": str(i.price_per_kg),
                "effectiveDate": i.effective_date.isoformat(),
                "sourceName": i.source_name,
                "sourceUrl": i.source_url,
            }
            for i in items
        ],
    )

    if args.dry_run:
        print(json.dumps({"run_id": run_id, "items": [i.material_type for i in items]}, ensure_ascii=False))
        return

    result = batch_upsert(session, cfg, args.backend, token, items, run_id=run_id)
    log("price_crawler.written", run_id=run_id, result=result)


if __name__ == "__main__":
    main()
