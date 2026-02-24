from __future__ import annotations

import os


def getenv(name: str, default: str | None = None) -> str | None:
    v = os.getenv(name)
    if v is None:
        return default
    v = v.strip()
    return v if v else default


def getenv_int(name: str, default: int) -> int:
    v = getenv(name)
    if v is None:
        return default
    return int(v)
