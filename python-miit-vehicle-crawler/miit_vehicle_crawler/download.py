from __future__ import annotations

import hashlib
import os
from datetime import datetime
from pathlib import Path
from urllib.parse import urljoin

from playwright.sync_api import BrowserContext, Page, sync_playwright

from .logging_utils import log
from .models import Candidate, PdfArchive


def download_from_urls(urls: list[str], out_dir: str, headful: bool = True) -> list[PdfArchive]:
    candidates = [
        Candidate(product_id=None, product_no=None, pdf_url=u, source_url=u, catalog_batch=None, catalog_index=None) for u in urls
    ]
    return download_from_candidates(candidates, out_dir=out_dir, headful=headful)


def download_from_candidates(candidates: list[Candidate], out_dir: str, headful: bool = True) -> list[PdfArchive]:
    out = []
    out_base = Path(out_dir)
    out_base.mkdir(parents=True, exist_ok=True)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=not headful)
        context = browser.new_context(accept_downloads=False)
        page = context.new_page()

        for c in candidates:
            try:
                archive = _download_candidate(context, page, c, out_base)
                out.append(archive)
                log("miit.downloaded", sha256=archive.sha256, local_path=archive.local_path, pdf_url=c.pdf_url)
            except Exception as e:
                log("miit.download_failed", pdf_url=c.pdf_url, error=str(e))

        context.close()
        browser.close()

    return out


def _download_candidate(context: BrowserContext, page: Page, c: Candidate, out_base: Path) -> PdfArchive:
    fetched_at = datetime.now()
    resp = page.goto(c.pdf_url, wait_until="domcontentloaded")
    if _captcha_present(page):
        log("miit.waiting_for_captcha", url=page.url)
        _wait_for_operator(page)
        resp = page.goto(page.url, wait_until="domcontentloaded")

    pdf_bytes = None
    if resp is not None:
        ct = (resp.headers.get("content-type") or "").lower()
        if "application/pdf" in ct or resp.url.lower().endswith(".pdf"):
            pdf_bytes = resp.body()

    if pdf_bytes is None:
        pdf_urls = _extract_pdf_urls(page)
        if not pdf_urls:
            raise RuntimeError("no pdf url found on page")
        pdf_bytes = _fetch_pdf_bytes(page, pdf_urls[0])

    sha = hashlib.sha256(pdf_bytes).hexdigest()
    day = fetched_at.strftime("%Y-%m-%d")
    target_dir = out_base / day
    target_dir.mkdir(parents=True, exist_ok=True)
    target_path = target_dir / f"{sha}.pdf"
    if not target_path.exists():
        target_path.write_bytes(pdf_bytes)

    return PdfArchive(candidate=c, sha256=sha, local_path=str(target_path.resolve()), fetched_at=fetched_at)


def _fetch_pdf_bytes(page: Page, pdf_url: str) -> bytes:
    resp = page.goto(pdf_url, wait_until="domcontentloaded")
    if resp is None:
        raise RuntimeError("no response for pdf url")
    ct = (resp.headers.get("content-type") or "").lower()
    if "application/pdf" not in ct and not resp.url.lower().endswith(".pdf"):
        raise RuntimeError(f"not a pdf response: {resp.url}")
    return resp.body()


def _extract_pdf_urls(page: Page) -> list[str]:
    urls: list[str] = []
    base = page.url

    for a in page.query_selector_all("a"):
        href = a.get_attribute("href")
        if href and ".pdf" in href.lower():
            urls.append(urljoin(base, href))

    for tag in ["iframe", "embed", "object"]:
        for el in page.query_selector_all(tag):
            src = el.get_attribute("src") or el.get_attribute("data")
            if src and ".pdf" in src.lower():
                urls.append(urljoin(base, src))

    seen = set()
    deduped = []
    for u in urls:
        if u not in seen:
            seen.add(u)
            deduped.append(u)
    return deduped


def _captcha_present(page: Page) -> bool:
    text = (page.inner_text("body") or "")[:2000]
    if "验证码" in text or "拖动" in text or "拼图" in text:
        return True
    u = (page.url or "").lower()
    return "captcha" in u


def _wait_for_operator(page: Page) -> None:
    if os.getenv("MIIT_NON_INTERACTIVE") == "1":
        raise RuntimeError("captcha present but non-interactive mode enabled")
    input("请在浏览器中完成验证码后按回车继续：")
