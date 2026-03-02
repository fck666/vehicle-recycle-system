from __future__ import annotations

import html as _html
import json
import re
import time
from dataclasses import dataclass
from datetime import datetime, date
from typing import Any
from urllib.parse import urljoin

import requests


BASE = "https://service.miit-eidc.org.cn/miitxxgk/gonggao/xxgk/"


@dataclass
class CpListItem:
    cpid: str
    pc: str
    data_tag: str
    qymc: str | None
    cpsb: str | None
    clxh: str | None
    clmc: str | None

    @property
    def detail_url(self) -> str:
        return f"{BASE}queryCpData?dataTag={self.data_tag}&gid={self.cpid}&pc={self.pc}"


def do_cp_query(
    session: requests.Session,
    qymc: str | None,
    pc: str | int | None,
    cpsb: str | None,
    clxh: str | None,
    clmc: str | None,
    page_num: int,
    page_size: int,
) -> dict[str, Any]:
    url = urljoin(BASE, "doCpQuery")
    data: dict[str, str] = {
        "qymc": qymc or "",
        "pc": str(pc) if pc is not None else "",
        "cpsb": cpsb or "",
        "clxh": clxh or "",
        "clmc": clmc or "",
        "pageSize": str(page_size),
        "pageNum": str(page_num),
    }
    resp = session.post(url, data=data, timeout=30, headers={"Referer": urljoin(BASE, "queryByPc?pc=396&querylb=cp&qyinfolb=")})
    resp.raise_for_status()
    return resp.json()


def iter_cp_list(
    session: requests.Session,
    qymc: str | None,
    pc: int,
    cpsb: str | None,
    clxh: str | None,
    clmc: str | None,
    page_size: int = 10,
) -> list[CpListItem]:
    first = do_cp_query(session, qymc=qymc, pc=pc, cpsb=cpsb, clxh=clxh, clmc=clmc, page_num=1, page_size=page_size)
    if (first.get("handleResult") or {}).get("respCode") != 200:
        return []
    
    count_result = first.get("countResult") or {}
    total_page = int(count_result.get("totalPage") or 1)
    total_count = int(count_result.get("total") or 0)
    
    # Debug log
    print(f"DEBUG: pc={pc} cpsb={cpsb} total={total_count} totalPage={total_page}")
    
    out: list[CpListItem] = []
    out.extend(_parse_cp_list(first.get("cpList") or []))
    
    # If there are more pages, fetch them
    for pn in range(2, total_page + 1):
        # Add a small delay to be polite
        time.sleep(0.5)
        row = do_cp_query(session, qymc=qymc, pc=pc, cpsb=cpsb, clxh=clxh, clmc=clmc, page_num=pn, page_size=page_size)
        out.extend(_parse_cp_list(row.get("cpList") or []))
    
    return out


def _parse_cp_list(items: list[dict[str, Any]]) -> list[CpListItem]:
    out: list[CpListItem] = []
    for x in items:
        cpid = str(x.get("cpid") or "").strip()
        pc = str(x.get("pc") or "").strip()
        data_tag = str(x.get("dataTag") or "Z").strip()
        if not cpid or not pc:
            continue
        out.append(
            CpListItem(
                cpid=cpid,
                pc=pc,
                data_tag=data_tag,
                qymc=_trim_or_none(x.get("qymc")),
                cpsb=_trim_or_none(x.get("cpsb")),
                clxh=_trim_or_none(x.get("clxh")),
                clmc=_trim_or_none(x.get("clmc")),
            )
        )
    return out


def parse_detail_html(html: str, detail_url: str) -> tuple[dict[str, str], list[str]]:
    field_map: dict[str, str] = {}

    s = html.replace("\r", "").replace("\n", "")
    rows = re.findall(r"<tr[^>]*>(.*?)</tr>", s, flags=re.I)
    for row in rows:
        tds = re.findall(r"<td[^>]*>(.*?)</td>", row, flags=re.I)
        texts = [_strip_tags(x) for x in tds]
        texts = [x for x in texts if x != ""]
        if len(texts) >= 2:
            for i in range(0, len(texts) - 1, 2):
                k = texts[i]
                v = texts[i + 1]
                if k:
                    field_map[k] = v

    img_srcs = re.findall(r"<img[^>]+src=\"([^\"]+)\"", s, flags=re.I)
    imgs: list[str] = []
    for src in img_srcs:
        if "getPic" in src and ("gid=" in src or "pc=" in src):
            imgs.append(urljoin(detail_url, src))
    imgs = list(dict.fromkeys(imgs))
    return field_map, imgs


def to_vehicle_spec_item(field_map: dict[str, str], detail_url: str, raw_html: str, source_site: str = "MIIT_EIDC") -> dict[str, Any]:
    product_no = field_map.get("产品号") or field_map.get("产品号 ")
    product_id = field_map.get("产品ID") or field_map.get("产品ID ")
    pc = field_map.get("批次")
    release_date = _parse_yyyymmdd(field_map.get("发布日期"))

    brand = field_map.get("产品商标")
    model_code = field_map.get("车辆型号")
    vehicle_type = field_map.get("车辆名称")
    manufacturer = field_map.get("企业名称")
    production_address = field_map.get("生产地址")

    curb_weight = _parse_num_max(field_map.get("整备质量"))
    gross_weight = _parse_num_max(field_map.get("总质量"))
    length_mm = _parse_int(field_map.get("外形尺寸长"))
    width_mm = _parse_int(field_map.get("外形尺寸宽"))
    height_mm = _parse_int_max(field_map.get("外形尺寸高"))
    wheelbase_mm = _parse_int(field_map.get("轴距"))
    max_speed = _parse_int(field_map.get("最高车速"))
    axle_count = _parse_int(field_map.get("轴数"))
    front_track = _parse_int(field_map.get("前轮距"))
    rear_track = _parse_int(field_map.get("后轮距"))
    tire_count = _parse_int(field_map.get("轮胎数"))
    tire_spec = field_map.get("轮胎规格")
    steering_type = field_map.get("转向形式")
    vin_pattern = field_map.get("车辆识别代号(VIN)")
    fuel_type = field_map.get("燃料种类")
    displacement = _parse_int(field_map.get("排量"))
    power_kw = _parse_num_max(field_map.get("发动机功率"))
    motor_model = field_map.get("发动机型号")
    motor_manufacturer = field_map.get("发动机生产企业")
    has_abs = _parse_bool_zh(field_map.get("防抱死系统"))
    axle_load = field_map.get("轴荷")
    approach_departure = field_map.get("接近角/离去角")
    approach_angle = None
    departure_angle = None
    if approach_departure and "/" in approach_departure:
        parts = approach_departure.split("/")
        if len(parts) >= 2:
            approach_angle = _parse_num(parts[0])
            departure_angle = _parse_num(parts[1])
    overhang = field_map.get("前悬后悬")
    front_overhang = None
    rear_overhang = None
    if overhang and "/" in overhang:
        parts = overhang.split("/")
        if len(parts) >= 2:
            front_overhang = _parse_int(parts[0])
            rear_overhang = _parse_int(parts[1])

    raw = {
        "sourceSite": source_site,
        "detailUrl": detail_url,
        "fetchedAt": datetime.now().isoformat(timespec="seconds"),
        "fieldMap": field_map,
        "rawHtml": raw_html,
    }

    item: dict[str, Any] = {
        "sourceSite": source_site,
        "productId": product_id,
        "productNo": product_no,
        "batchNo": int(pc) if pc and pc.isdigit() else None,
        "releaseDate": release_date,
        "brand": brand,
        "model": model_code,
        "vehicleType": vehicle_type,
        "manufacturerName": manufacturer,
        "trademark": brand,
        "productionAddress": production_address,
        "productModel": model_code,
        "curbWeight": curb_weight,
        "grossWeight": gross_weight,
        "lengthMm": length_mm,
        "widthMm": width_mm,
        "heightMm": height_mm,
        "wheelbaseMm": wheelbase_mm,
        "frontOverhangMm": front_overhang,
        "rearOverhangMm": rear_overhang,
        "approachAngleDeg": approach_angle,
        "departureAngleDeg": departure_angle,
        "frontTrackMm": front_track,
        "rearTrackMm": rear_track,
        "axleCount": axle_count,
        "axleLoadKg": axle_load,
        "tireCount": tire_count,
        "tireSpec": tire_spec,
        "steeringType": steering_type,
        "hasAbs": has_abs,
        "maxSpeedKmh": max_speed,
        "fuelType": fuel_type,
        "displacementMl": displacement,
        "powerKw": power_kw,
        "motorModel": motor_model,
        "motorManufacturer": motor_manufacturer,
        "vinPattern": vin_pattern,
        "specRawJson": json.dumps(raw, ensure_ascii=False),
    }
    item = {k: v for k, v in item.items() if v is not None and v != ""}
    return item


def _strip_tags(s: str) -> str:
    if s is None:
        return ""
    t = re.sub(r"<[^>]+>", "", s)
    t = _html.unescape(t)
    t = t.replace("\xa0", " ").strip()
    return t


def _trim_or_none(v: Any) -> str | None:
    if v is None:
        return None
    s = str(v).strip()
    return s if s else None


def _parse_yyyymmdd(s: str | None) -> str | None:
    if not s:
        return None
    t = re.sub(r"[^0-9]", "", s)
    if len(t) == 8:
        try:
            d = date(int(t[0:4]), int(t[4:6]), int(t[6:8]))
            return d.isoformat()
        except Exception:
            return None
    return None


def _parse_num(s: str | None) -> float | None:
    if not s:
        return None
    t = re.findall(r"[-+]?[0-9]+(?:\\.[0-9]+)?", s)
    if not t:
        return None
    try:
        return float(t[0])
    except Exception:
        return None


def _parse_num_max(s: str | None) -> float | None:
    if not s:
        return None
    nums = re.findall(r"[-+]?[0-9]+(?:\\.[0-9]+)?", s.replace(",", " "))
    if not nums:
        return None
    try:
        return float(max(float(x) for x in nums))
    except Exception:
        return None


def _parse_int(s: str | None) -> int | None:
    if not s:
        return None
    nums = re.findall(r"[-+]?[0-9]+", s)
    if not nums:
        return None
    try:
        return int(nums[0])
    except Exception:
        return None


def _parse_int_max(s: str | None) -> int | None:
    if not s:
        return None
    nums = re.findall(r"[-+]?[0-9]+", s.replace(",", " "))
    if not nums:
        return None
    try:
        return int(max(int(x) for x in nums))
    except Exception:
        return None


def _parse_bool_zh(s: str | None) -> bool | None:
    if not s:
        return None
    t = s.strip()
    if t in {"有", "是", "1", "Y", "y", "true", "TRUE"}:
        return True
    if t in {"无", "否", "0", "N", "n", "false", "FALSE"}:
        return False
    return None
