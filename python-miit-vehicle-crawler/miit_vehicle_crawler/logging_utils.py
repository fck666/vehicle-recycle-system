from __future__ import annotations

import json
import sys
from datetime import datetime, timezone
from typing import Any


def log(event: str, **fields: Any) -> None:
    payload = {"event": event, "ts": datetime.now(timezone.utc).isoformat()}
    payload.update(fields)
    sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
    sys.stdout.flush()
