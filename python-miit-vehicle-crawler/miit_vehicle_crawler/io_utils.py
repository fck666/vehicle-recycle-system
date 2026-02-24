from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Iterable, Iterator


def read_lines(path: str) -> list[str]:
    p = Path(path)
    out: list[str] = []
    for line in p.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        out.append(s)
    return out


def write_jsonl(path: str, rows: Iterable[dict[str, Any]]) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    with p.open("w", encoding="utf-8") as f:
        for row in rows:
            f.write(json.dumps(row, ensure_ascii=False) + "\n")


def iter_jsonl(path: str) -> Iterator[dict[str, Any]]:
    p = Path(path)
    with p.open("r", encoding="utf-8") as f:
        for line in f:
            s = line.strip()
            if not s:
                continue
            yield json.loads(s)
