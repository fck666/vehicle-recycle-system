import argparse
import json
import sys
import urllib.request


def _post_json(url: str, data: dict, token: str | None):
    body = json.dumps(data, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json; charset=utf-8")
    req.add_header("Accept", "application/json")
    req.add_header("User-Agent", "vehicle-recycle-system/python-dcd-vehicle-crawler")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req, timeout=60) as resp:
        raw = resp.read().decode("utf-8", "ignore")
        return resp.status, raw


def import_series(args: argparse.Namespace) -> int:
    backend = args.backend.rstrip("/")
    url = f"{backend}/api/admin/external-trims/dcd/import"
    series_ids = [int(x.strip()) for x in args.series_ids.split(",") if x.strip()]
    payload = {"seriesIds": series_ids, "cityName": args.city}
    code, raw = _post_json(url, payload, args.token)
    sys.stdout.write(raw + "\n")
    return 0 if 200 <= code < 300 else 1


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)

    p_import = sub.add_parser("import_series")
    p_import.add_argument("--backend", required=True)
    p_import.add_argument("--token", required=False)
    p_import.add_argument("--city", required=False, default="北京")
    p_import.add_argument("--series-ids", required=True)
    p_import.set_defaults(func=import_series)

    args = p.parse_args()
    raise SystemExit(args.func(args))


if __name__ == "__main__":
    main()

