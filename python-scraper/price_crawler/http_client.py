from __future__ import annotations

import time
from dataclasses import dataclass
from typing import Any

import requests


@dataclass(frozen=True)
class HttpConfig:
    timeout_seconds: int = 20
    max_retries: int = 3
    backoff_seconds: float = 0.8


def new_session() -> requests.Session:
    s = requests.Session()
    s.headers.update(
        {
            "User-Agent": "vehicle-recycle-system/price-crawler",
            "Accept": "text/html,application/json;q=0.9,*/*;q=0.8",
        }
    )
    return s


def get_text(session: requests.Session, url: str, cfg: HttpConfig) -> str:
    last_err: Exception | None = None
    for i in range(cfg.max_retries):
        try:
            resp = session.get(url, timeout=cfg.timeout_seconds)
            resp.raise_for_status()
            resp.encoding = resp.apparent_encoding or resp.encoding
            return resp.text
        except Exception as e:
            last_err = e
            if i + 1 < cfg.max_retries:
                time.sleep(cfg.backoff_seconds * (2**i))
    assert last_err is not None
    raise last_err


def post_json(session: requests.Session, url: str, payload: Any, cfg: HttpConfig) -> requests.Response:
    last_err: Exception | None = None
    for i in range(cfg.max_retries):
        try:
            resp = session.post(url, json=payload, timeout=cfg.timeout_seconds)
            resp.raise_for_status()
            return resp
        except Exception as e:
            last_err = e
            if i + 1 < cfg.max_retries:
                time.sleep(cfg.backoff_seconds * (2**i))
    assert last_err is not None
    raise last_err
