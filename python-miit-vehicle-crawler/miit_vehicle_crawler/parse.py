from __future__ import annotations

import json
import re
from dataclasses import asdict
from datetime import date, datetime
from decimal import Decimal
from pathlib import Path

import pdfplumber

from .logging_utils import log
from .models import ParsedSpec, PdfArchive


PARSER_VERSION = "miit_pdf_v1"


def parse_archives(archives: list[PdfArchive]) -> list[ParsedSpec]:
    out: list[ParsedSpec] = []
    for a in archives:
        try:
            spec = parse_pdf(a.local_path)
            out.append(spec)
            log("miit.parsed", sha256=a.sha256, product_id=spec.product_id, product_no=spec.product_no)
        except Exception as e:
            log("miit.parse_failed", sha256=a.sha256, local_path=a.local_path, error=str(e))
    return out


def parse_pdf(pdf_path: str) -> ParsedSpec:
    text = _extract_text(pdf_path)
    issues: list[str] = []
    confidence: dict[str, float] = {}

    if len(text.strip()) < 200:
        issues.append("TEXT_LAYER_MISSING_OR_TOO_SHORT")

    kv = _extract_kv(text)
    fields: dict = {}

    def put(key: str, value, conf: float) -> None:
        if value is None:
            return
        fields[key] = value
        confidence[key] = conf

    put("productNo", kv.get("product_no"), 0.95 if kv.get("product_no") else 0.0)
    put("productId", kv.get("product_id"), 0.95 if kv.get("product_id") else 0.0)
    put("productModel", kv.get("product_model"), 0.85 if kv.get("product_model") else 0.0)
    put("manufacturerName", kv.get("manufacturer_name"), 0.85 if kv.get("manufacturer_name") else 0.0)
    put("trademark", kv.get("trademark"), 0.8 if kv.get("trademark") else 0.0)
    put("productionAddress", kv.get("production_address"), 0.7 if kv.get("production_address") else 0.0)
    put("registrationAddress", kv.get("registration_address"), 0.7 if kv.get("registration_address") else 0.0)

    put("releaseDate", kv.get("release_date"), 0.8 if kv.get("release_date") else 0.0)
    put("effectiveDate", kv.get("effective_date"), 0.8 if kv.get("effective_date") else 0.0)
    put("batchNo", kv.get("batch_no"), 0.7 if kv.get("batch_no") else 0.0)
    put("catalogIndex", kv.get("catalog_index"), 0.7 if kv.get("catalog_index") else 0.0)

    for k in [
        "lengthMm",
        "widthMm",
        "heightMm",
        "wheelbaseMm",
        "frontOverhangMm",
        "rearOverhangMm",
        "frontTrackMm",
        "rearTrackMm",
        "axleCount",
        "tireCount",
        "maxSpeedKmh",
        "displacementMl",
    ]:
        if kv.get(k) is not None:
            put(k, kv.get(k), 0.75)

    for k in [
        "approachAngleDeg",
        "departureAngleDeg",
        "curbWeight",
        "grossWeight",
        "batteryKwh",
        "powerKw",
    ]:
        if kv.get(k) is not None:
            put(k, kv.get(k), 0.75)

    for k in [
        "axleLoadKg",
        "tireSpec",
        "steeringType",
        "hasAbs",
        "fuelType",
        "motorModel",
        "motorManufacturer",
        "vinPattern",
        "chassisModel",
    ]:
        if kv.get(k) is not None:
            put(k, kv.get(k), 0.7)

    product_id = fields.get("productId")
    product_no = fields.get("productNo")

    release_date = fields.get("releaseDate")
    effective_date = fields.get("effectiveDate")

    raw = {
        "parserVersion": PARSER_VERSION,
        "extractedAt": datetime.now().isoformat(timespec="seconds"),
        "kv": kv,
        "confidence": confidence,
        "issues": issues,
        "rawTextPreview": text[:2000],
        "pdfPath": str(Path(pdf_path).resolve()),
    }

    return ParsedSpec(
        product_id=product_id,
        product_no=product_no,
        fields=fields,
        raw_text=text,
        confidence=confidence,
        issues=issues,
        parser_version=PARSER_VERSION,
        source_site="miit",
        effective_date=effective_date,
        release_date=release_date,
    )


def to_vehicle_spec_item(spec: ParsedSpec, spec_raw_json_extra: dict | None = None) -> dict:
    item = {}
    for k, v in spec.fields.items():
        item[k] = _jsonify_value(v)
    item["sourceSite"] = spec.source_site

    raw = {
        "parserVersion": spec.parser_version,
        "issues": spec.issues,
        "confidence": spec.confidence,
    }
    if spec_raw_json_extra:
        raw.update(spec_raw_json_extra)
    item["specRawJson"] = json.dumps(raw, ensure_ascii=False, separators=(",", ":"))
    return item


def _extract_text(pdf_path: str) -> str:
    parts: list[str] = []
    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            t = page.extract_text() or ""
            parts.append(t)
    return "\n".join(parts)


def _extract_kv(text: str) -> dict:
    t = _normalize_text(text)
    kv: dict = {}

    kv["product_id"] = _m(t, r"产品ID[:：]\s*([A-Z0-9]+)")
    kv["product_no"] = _m(t, r"产品号[:：]\s*([A-Z0-9-]+)")
    kv["product_model"] = _m(t, r"产品型号/?名称[:：]\s*([^\n]+)")
    kv["manufacturer_name"] = _m(t, r"(企业名称|生产企业)[:：]\s*([^\n]+)", group=2)
    kv["trademark"] = _m(t, r"(商品商标|商标)[:：]\s*([^\n]+)", group=2)
    kv["production_address"] = _m(t, r"生产地址[:：]\s*([^\n]+)")
    kv["registration_address"] = _m(t, r"注册地址[:：]\s*([^\n]+)")

    kv["release_date"] = _parse_date(_m(t, r"发布日期[:：]\s*([0-9]{4}[-/.][0-9]{1,2}[-/.][0-9]{1,2})"))
    kv["effective_date"] = _parse_date(_m(t, r"生效日期[:：]\s*([0-9]{4}[-/.][0-9]{1,2}[-/.][0-9]{1,2})"))
    kv["batch_no"] = _parse_int(_m(t, r"(批次|批次号)[:：]\s*([0-9]{1,4})", group=2))
    kv["catalog_index"] = _parse_int(_m(t, r"(目录序号|序号)[:：]\s*([0-9]{1,6})", group=2))

    dims = _m(t, r"外形尺寸[^\n]*?([0-9]{3,5})\s*[×xX*/／]\s*([0-9]{3,5})\s*[×xX*/／]\s*([0-9]{3,5})")
    if dims:
        m = re.search(r"([0-9]{3,5})\s*[×xX*/／]\s*([0-9]{3,5})\s*[×xX*/／]\s*([0-9]{3,5})", dims)
        if m:
            kv["lengthMm"] = int(m.group(1))
            kv["widthMm"] = int(m.group(2))
            kv["heightMm"] = int(m.group(3))

    kv["wheelbaseMm"] = _parse_int(_m(t, r"轴距[^\n]*?([0-9]{3,5})"))
    kv["frontOverhangMm"] = _parse_int(_m(t, r"前悬[^\n]*?([0-9]{2,5})"))
    kv["rearOverhangMm"] = _parse_int(_m(t, r"后悬[^\n]*?([0-9]{2,5})"))
    kv["approachAngleDeg"] = _parse_decimal(_m(t, r"接近角[^\n]*?([0-9]+(?:\.[0-9]+)?)"))
    kv["departureAngleDeg"] = _parse_decimal(_m(t, r"离去角[^\n]*?([0-9]+(?:\.[0-9]+)?)"))
    kv["frontTrackMm"] = _parse_int(_m(t, r"前轮距[^\n]*?([0-9]{2,5})"))
    kv["rearTrackMm"] = _parse_int(_m(t, r"后轮距[^\n]*?([0-9]{2,5})"))

    kv["axleCount"] = _parse_int(_m(t, r"轴数[^\n]*?([0-9]{1,2})"))
    kv["axleLoadKg"] = _m(t, r"轴荷[^\n]*?([0-9/／]+)")
    kv["tireCount"] = _parse_int(_m(t, r"轮胎数[^\n]*?([0-9]{1,2})"))
    kv["tireSpec"] = _m(t, r"轮胎规格[^\n]*?([^\n]+)")
    kv["steeringType"] = _m(t, r"转向型式[^\n]*?([^\n]+)")
    abs_flag = _m(t, r"(防抱死系统|ABS)[^\n]*?((有|无|装配|未装配|具备|不具备))", group=2)
    if abs_flag:
        kv["hasAbs"] = abs_flag in {"有", "装配", "具备"}
    kv["maxSpeedKmh"] = _parse_int(_m(t, r"最高车速[^\n]*?([0-9]{2,3})"))

    kv["curbWeight"] = _parse_decimal(_m(t, r"整备质量[^\n]*?([0-9]+(?:\.[0-9]+)?)"))
    kv["grossWeight"] = _parse_decimal(_m(t, r"总质量[^\n]*?([0-9]+(?:\.[0-9]+)?)"))
    kv["batteryKwh"] = _parse_decimal(_m(t, r"(电池容量|电池总能量)[^\n]*?([0-9]+(?:\.[0-9]+)?)", group=2))

    kv["fuelType"] = _m(t, r"燃料种类[^\n]*?([^\n]+)")
    kv["motorModel"] = _m(t, r"(发动机|电机)型号[^\n]*?([^\n]+)", group=2)
    kv["motorManufacturer"] = _m(t, r"(生产企业|制造企业)[^\n]*?([^\n]+)", group=2)
    kv["displacementMl"] = _parse_int(_m(t, r"排量[^\n]*?([0-9]{3,6})"))
    kv["powerKw"] = _parse_decimal(_m(t, r"功率[^\n]*?([0-9]+(?:\.[0-9]+)?)"))

    kv["vinPattern"] = _m(t, r"(车辆识别代号|VIN)[^\n]*?([A-Z0-9\*\-]{6,20})", group=2)
    kv["chassisModel"] = _m(t, r"底盘型号[^\n]*?([^\n]+)")
    return kv


def _normalize_text(text: str) -> str:
    return text.replace("\u3000", " ").replace("\r", "\n")


def _m(text: str, pattern: str, group: int = 1) -> str | None:
    m = re.search(pattern, text)
    if not m:
        return None
    return (m.group(group) or "").strip()


def _parse_int(s: str | None) -> int | None:
    if not s:
        return None
    s = re.sub(r"[^\d]", "", s)
    if not s:
        return None
    return int(s)


def _parse_decimal(s: str | None) -> Decimal | None:
    if not s:
        return None
    s = s.strip()
    s = s.replace("，", ".").replace(",", ".")
    m = re.search(r"([0-9]+(?:\.[0-9]+)?)", s)
    if not m:
        return None
    return Decimal(m.group(1))


def _parse_date(s: str | None) -> date | None:
    if not s:
        return None
    s = s.strip().replace("/", "-").replace(".", "-")
    parts = s.split("-")
    if len(parts) != 3:
        return None
    y, m, d = parts
    try:
        return date(int(y), int(m), int(d))
    except Exception:
        return None


def _jsonify_value(v):
    if isinstance(v, date):
        return v.isoformat()
    if isinstance(v, Decimal):
        return float(v)
    return v
