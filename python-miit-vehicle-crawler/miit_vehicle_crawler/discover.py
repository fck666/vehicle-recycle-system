from __future__ import annotations

import os
from urllib.parse import urljoin

from playwright.sync_api import sync_playwright

from .logging_utils import log


MIIT_INDEX_URL = "https://service.miit-eidc.org.cn/miitxxgk/gonggao/xxgk/index"


def interactive_discover(output_jsonl: str, headful: bool = True) -> int:
    if os.getenv("MIIT_NON_INTERACTIVE") == "1":
        raise RuntimeError("discover requires interactive mode")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=not headful)
        context = browser.new_context()
        page = context.new_page()
        page.goto(MIIT_INDEX_URL, wait_until="domcontentloaded")

        log("miit.discover.opened", url=MIIT_INDEX_URL)
        input("请在浏览器中完成筛选/搜索，并停留在结果列表页后按回车导出：")

        base = page.url
        rows = []
        for a in page.query_selector_all("a"):
            href = a.get_attribute("href")
            if not href:
                continue
            text = (a.inner_text() or "").strip()
            abs_url = urljoin(base, href)
            if "javascript:" in abs_url.lower():
                continue
            rows.append({"pdf_url": abs_url, "source_url": base, "anchor_text": text})

        from .io_utils import write_jsonl

        write_jsonl(output_jsonl, rows)

        context.close()
        browser.close()

    return len(rows)

