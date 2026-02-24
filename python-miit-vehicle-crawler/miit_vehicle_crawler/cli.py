from __future__ import annotations

import argparse
import json
import os
import uuid
from datetime import datetime

import requests

from .backend_api import upsert_vehicle_documents, upsert_vehicle_specs
from .discover import interactive_discover
from .download import download_from_candidates, download_from_urls
from .io_utils import iter_jsonl, read_lines, write_jsonl
from .logging_utils import log
from .models import Candidate, PdfArchive
from .parse import parse_pdf, to_vehicle_spec_item


def main() -> None:
    parser = argparse.ArgumentParser(prog="miit-vehicle-crawler")
    sub = parser.add_subparsers(dest="cmd", required=True)

    d = sub.add_parser("discover")
    d.add_argument("--output", default="data/candidates.jsonl")
    d.add_argument("--headless", action="store_true")

    dl = sub.add_parser("download")
    dl.add_argument("--urls", help="txt file, one url per line")
    dl.add_argument("--candidates-jsonl", help="jsonl file containing pdf_url and optional productId/productNo")
    dl.add_argument("--out-dir", default="data/pdfs")
    dl.add_argument("--archives-out", default="data/archives.jsonl")
    dl.add_argument("--headless", action="store_true")

    p = sub.add_parser("parse")
    p.add_argument("--archives-jsonl", default="data/archives.jsonl")
    p.add_argument("--output", default="data/specs.jsonl")

    u = sub.add_parser("upsert")
    u.add_argument("--spec-jsonl", default="data/specs.jsonl")
    u.add_argument("--backend", default=os.getenv("BACKEND_BASE_URL", "http://localhost:8080"))
    u.add_argument("--token", default=os.getenv("BACKEND_TOKEN", ""))

    args = parser.parse_args()

    if args.cmd == "discover":
        n = interactive_discover(args.output, headful=not args.headless)
        log("miit.discover.done", count=n, output=args.output)
        return

    if args.cmd == "download":
        run_download(args)
        return

    if args.cmd == "parse":
        run_parse(args)
        return

    if args.cmd == "upsert":
        run_upsert(args)
        return


def run_download(args: argparse.Namespace) -> None:
    run_id = str(uuid.uuid4())
    headful = not bool(args.headless)

    if args.urls:
        urls = read_lines(args.urls)
        log("miit.download.start", run_id=run_id, mode="urls", count=len(urls), out_dir=args.out_dir)
        archives = download_from_urls(urls, out_dir=args.out_dir, headful=headful)
    elif args.candidates_jsonl:
        candidates = []
        for row in iter_jsonl(args.candidates_jsonl):
            candidates.append(
                Candidate(
                    product_id=row.get("productId") or row.get("product_id"),
                    product_no=row.get("productNo") or row.get("product_no"),
                    pdf_url=row.get("pdf_url") or row.get("pdfUrl") or row.get("url"),
                    source_url=row.get("source_url") or row.get("sourceUrl"),
                    catalog_batch=row.get("catalog_batch") or row.get("catalogBatch"),
                    catalog_index=row.get("catalog_index") or row.get("catalogIndex"),
                )
            )
        log("miit.download.start", run_id=run_id, mode="candidates", count=len(candidates), out_dir=args.out_dir)
        archives = download_from_candidates(candidates, out_dir=args.out_dir, headful=headful)
    else:
        raise SystemExit("must provide --urls or --candidates-jsonl")

    rows = []
    for a in archives:
        rows.append(
            {
                "sha256": a.sha256,
                "localPath": a.local_path,
                "pdfUrl": a.candidate.pdf_url,
                "sourceUrl": a.candidate.source_url,
                "productId": a.candidate.product_id,
                "productNo": a.candidate.product_no,
                "catalogBatch": a.candidate.catalog_batch,
                "catalogIndex": a.candidate.catalog_index,
                "fetchedAt": a.fetched_at.isoformat(timespec="seconds"),
            }
        )
    write_jsonl(args.archives_out, rows)
    log("miit.download.done", run_id=run_id, count=len(rows), archives_out=args.archives_out)


def run_parse(args: argparse.Namespace) -> None:
    run_id = str(uuid.uuid4())
    log("miit.parse.start", run_id=run_id, archives=args.archives_jsonl, output=args.output)

    rows = []
    for row in iter_jsonl(args.archives_jsonl):
        pdf_path = row["localPath"]
        spec = parse_pdf(pdf_path)

        spec_extra = {
            "pdf": {
                "sha256": row.get("sha256"),
                "localPath": pdf_path,
                "pdfUrl": row.get("pdfUrl"),
                "sourceUrl": row.get("sourceUrl"),
                "fetchedAt": row.get("fetchedAt"),
            },
            "catalog": {"batch": row.get("catalogBatch"), "index": row.get("catalogIndex")},
        }
        item = to_vehicle_spec_item(spec, spec_raw_json_extra=spec_extra)

        doc = {
            "productId": item.get("productId"),
            "productNo": item.get("productNo"),
            "docType": "MIIT_PDF",
            "docName": row.get("sha256") + ".pdf" if row.get("sha256") else None,
            "docUrl": "file://" + pdf_path,
            "sha256": row.get("sha256"),
            "sourceUrl": row.get("pdfUrl") or row.get("sourceUrl"),
            "fetchedAt": row.get("fetchedAt"),
        }
        rows.append({"vehicleSpec": item, "vehicleDocument": doc})

    write_jsonl(args.output, rows)
    log("miit.parse.done", run_id=run_id, count=len(rows), output=args.output)


def run_upsert(args: argparse.Namespace) -> None:
    run_id = str(uuid.uuid4())
    backend = args.backend
    token = args.token.strip() if args.token else ""
    log("miit.upsert.start", run_id=run_id, backend=backend, spec_jsonl=args.spec_jsonl)

    spec_items = []
    doc_items = []
    for row in iter_jsonl(args.spec_jsonl):
        spec = row.get("vehicleSpec") or {}
        doc = row.get("vehicleDocument") or {}
        spec_items.append(spec)
        doc_items.append(doc)

    session = requests.Session()
    if token:
        session.headers.update({"Authorization": f"Bearer {token}"})

    spec_result = upsert_vehicle_specs(session, backend, spec_items)
    doc_result = upsert_vehicle_documents(session, backend, doc_items)

    log("miit.upsert.done", run_id=run_id, spec_result=spec_result, doc_result=doc_result)
    print(json.dumps({"spec": spec_result, "documents": doc_result, "run_id": run_id}, ensure_ascii=False))


if __name__ == "__main__":
    main()

