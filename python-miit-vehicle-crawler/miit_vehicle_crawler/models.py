from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime


@dataclass(frozen=True)
class Candidate:
    product_id: str | None
    product_no: str | None
    pdf_url: str
    source_url: str | None
    catalog_batch: int | None = None
    catalog_index: int | None = None


@dataclass(frozen=True)
class PdfArchive:
    candidate: Candidate
    sha256: str
    local_path: str
    fetched_at: datetime


@dataclass(frozen=True)
class ParsedSpec:
    product_id: str | None
    product_no: str | None
    fields: dict
    raw_text: str
    confidence: dict
    issues: list[str]
    parser_version: str
    source_site: str
    effective_date: date | None
    release_date: date | None
