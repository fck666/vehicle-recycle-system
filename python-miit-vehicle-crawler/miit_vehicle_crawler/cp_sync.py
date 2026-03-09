from __future__ import annotations

import hashlib
import os
import random
import tempfile
import time
from datetime import datetime
from typing import Any
from urllib.parse import quote

import cv2
import numpy as np
import requests

from .backend_api import upsert_vehicle_documents, upsert_vehicle_specs
from .logging_utils import log
from .miit_eidc import CpListItem, iter_cp_list, parse_detail_html, to_vehicle_spec_item


class CaptchaSolver:
    @staticmethod
    def identify_gap(bg_bytes, slider_bytes):
        try:
            bg_arr = np.frombuffer(bg_bytes, np.uint8)
            slider_arr = np.frombuffer(slider_bytes, np.uint8)
            bg_img = cv2.imdecode(bg_arr, cv2.IMREAD_COLOR)
            slider_img = cv2.imdecode(slider_arr, cv2.IMREAD_COLOR)

            bg_gray = cv2.GaussianBlur(cv2.cvtColor(bg_img, cv2.COLOR_BGR2GRAY), (5, 5), 0)
            slider_gray = cv2.GaussianBlur(cv2.cvtColor(slider_img, cv2.COLOR_BGR2GRAY), (5, 5), 0)

            bg_edge = cv2.Canny(bg_gray, 100, 200)
            slider_edge = cv2.Canny(slider_gray, 100, 200)

            res = cv2.matchTemplate(bg_edge, slider_edge, cv2.TM_CCOEFF_NORMED)
            min_val, max_val, min_loc, max_loc = cv2.minMaxLoc(res)
            return max_loc[0]
        except Exception as e:
            log("captcha.identify.error", err=str(e))
            return 0

    @staticmethod
    def get_track(distance):
        track = []
        current = 0
        mid = distance * 4 / 5
        t = 0.2
        v = 0
        while current < distance:
            if current < mid:
                a = 2
            else:
                a = -3
            v0 = v
            v = v0 + a * t
            move = v0 * t + 0.5 * a * t * t
            current += move
            track.append(round(move))
        return track

    @staticmethod
    def solve(page, max_retries=3):
        for i in range(max_retries):
            try:
                # Check if captcha exists
                txt = page.content()
                if "拖动滑块" not in txt and "拼图" not in txt:
                    return True

                log("captcha.detected", attempt=i + 1)
                
                page.wait_for_timeout(2000)
                
                # Based on user provided HTML
                # Slider knob: .drag-block
                # Background image (with gap): .check-block (style background-position)
                # Slider piece image: .check-content (style left/top)
                # Note: The images seem to be CSS sprites or background images
                # But looking at the HTML:
                # <div class="check-block" style="background-position: -27px -130px; top: 130px;"></div>
                # This suggests the background image is set via CSS on .check-block or its parent
                
                # Let's inspect the style of .check-block to find the image URL
                
                knob = page.locator(".drag-block").first
                bg_elem = page.locator(".check-block").first
                slider_elem = page.locator(".check-content").first
                
                if knob.is_visible() and bg_elem.is_visible():
                    log("captcha.elements.found")
                    
                    # Get background image URL from computed style
                    # The image is likely set on .check-block or inherited
                    # Actually, usually in these custom captchas, there is a common background image
                    # Let's try to get it from .check-block background-image
                    
                    bg_url = bg_elem.evaluate("element => window.getComputedStyle(element).backgroundImage")
                    # format: url("...")
                    if bg_url and "url" in bg_url:
                        bg_url = bg_url.strip()[4:-1].replace('"', '').replace("'", "")
                        if not bg_url.startswith("http") and not bg_url.startswith("data:"):
                             bg_url = requests.compat.urljoin(page.url, bg_url)
                    else:
                        # Try to find any image inside check if it's an img tag (user html showed divs but maybe style is applied)
                        # User HTML: <div class="check-block" style="..."></div>
                        # So it must be background-image.
                        # If getComputedStyle fails or is none, check parent .check
                        pass
                        
                    # Wait, the user HTML shows:
                    # <div class="check-block" style="background-position: -27px -130px; top: 130px;"></div>
                    # It doesn't show where the image comes from. It might be defined in CSS for .check-block or .check
                    # Let's try to get it from .check-block
                    
                    if not bg_url or bg_url == "none":
                         # Fallback: maybe it's on the parent .check or .con?
                         # Or maybe it's a separate img tag not in the snippet?
                         # User snippet: <div class="check"> ... </div>
                         pass

                    # If we can't get the image, we can't solve it with CV.
                    # However, looking at the script:
                    # let x = random(0, 325)
                    # checkContent.style.cssText = `left:${x}px;top:${y}px`
                    # checkBlock.style.cssText = `background-position: -${x}px -${y}px;top: ${y}px`
                    
                    # Wait! This is a client-side generated captcha?
                    # "let x = random(0, 325)" is in the script tag in the HTML!
                    # If x is generated on the client side (in the browser), we can just read it from the DOM!
                    
                    # The script sets: checkContent.style.left = x + 'px'
                    # So x = parseInt(checkContent.style.left)
                    
                    # Let's try to read the 'left' style from .check-content
                    target_left = slider_elem.evaluate("element => element.style.left")
                    if target_left and "px" in target_left:
                        target_x = float(target_left.replace("px", ""))
                        log("captcha.target.found_in_dom", x=target_x)
                        
                        # The distance to drag is target_x
                        # Script says: if (offsetX >= x-2 && offsetX <= x+2)
                        # So we just need to drag 'target_x' pixels.
                        
                        distance = target_x
                        log("captcha.solving", distance=distance, start_x=knob.bounding_box()['x'])
                        
                        knob_box = knob.bounding_box()
                        start_x = knob_box['x'] + knob_box['width'] / 2
                        start_y = knob_box['y'] + knob_box['height'] / 2
                        
                        # Human-like movement preparation
                        page.mouse.move(start_x, start_y)
                        time.sleep(random.uniform(0.2, 0.5))
                        page.mouse.down()
                        time.sleep(random.uniform(0.1, 0.3))
                        
                        tracks = CaptchaSolver.get_track(distance)
                        current_x = start_x
                        for t in tracks:
                            current_x += t
                            # More natural y-jitter
                            jitter_y = random.randint(-1, 1) if random.random() > 0.5 else 0
                            page.mouse.move(current_x, start_y + jitter_y)
                            # Variable speed
                            time.sleep(random.uniform(0.005, 0.02))
                        
                        # Overshoot and correct (human behavior)
                        if random.random() > 0.3:
                            overshoot = random.randint(2, 5)
                            page.mouse.move(start_x + distance + overshoot, start_y)
                            time.sleep(random.uniform(0.1, 0.2))
                            page.mouse.move(start_x + distance, start_y)
                        else:
                            page.mouse.move(start_x + distance, start_y)
                            
                        time.sleep(random.uniform(0.2, 0.5))
                        page.mouse.up()
                        
                        # Wait for result
                        page.wait_for_timeout(3000)
                        
                        # Check if success - check if submit button is enabled
                        # HTML: <button ... id="submit-btn" disabled="">继续访问</button>
                        submit_btn = page.locator("#submit-btn")
                        
                        # Wait a bit for JS to validate and enable button
                        page.wait_for_timeout(1000)
                        
                        if submit_btn.is_visible() and not submit_btn.is_disabled():
                            log("captcha.solved.clicking_submit")
                            submit_btn.click()
                            page.wait_for_timeout(2000)
                            
                            txt_after = page.content()
                            if "拖动滑块" not in txt_after and "拼图" not in txt_after:
                                log("captcha.solve.success")
                                return True
                            else:
                                log("captcha.submit.failed")
                        else:
                            log("captcha.solve.failed_validation")
                            # Refresh page to get a new captcha state
                            try:
                                page.reload()
                                page.wait_for_timeout(2000)
                            except:
                                pass

                            
                    else:
                        log("captcha.target.not_found_in_dom")
                        # Fallback to CV if DOM read fails (e.g. if x is not in inline style)
                        # But based on the user provided script, it puts it in inline style.
                
                else:
                    log("captcha.elements.not_found")
                    break

            except Exception as e:
                log("captcha.solve.error", err=str(e))
                time.sleep(2)
        
        return False



def sync_cp(
    backend: str,
    token: str,
    pc_from: int = 398,
    pc_to: int = 398,
    qymc: str | None = None,
    qymc_list: list[str] | None = None,
    cpsb: str | None = None,
    cpsb_list: list[str] | None = None,
    clxh: str | None = None,
    clmc: str | None = None,
    page_size: int = 10,
    headful: bool = False,
    limit: int = 0,
    on_progress=None,
    run_id: str | None = None,
    retry_items: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    session = requests.Session()
    if token:
        session.headers.update({"Authorization": f"Bearer {token}"})

    items: list[CpListItem] = []
    
    if retry_items:
        # Direct retry mode: bypass search
        log("miit.cp.mode.retry", count=len(retry_items))
        for x in retry_items:
            # Reconstruct CpListItem from dict
            # We expect x to contain keys matching CpListItem fields or raw dict
            cpid = str(x.get("cpid") or "").strip()
            pc = str(x.get("pc") or "").strip()
            if not cpid or not pc:
                continue
            items.append(
                CpListItem(
                    cpid=cpid,
                    pc=pc,
                    data_tag=str(x.get("dataTag") or "Z").strip(),
                    qymc=x.get("qymc"),
                    cpsb=x.get("cpsb"),
                    clxh=x.get("clxh"),
                    clmc=x.get("clmc"),
                )
            )
    else:
        # Search mode
        search_configs = []
        if qymc_list:
            for q in qymc_list:
                search_configs.append({"qymc": q, "cpsb": cpsb})
        else:
            brands = []
            if cpsb_list:
                brands.extend([x for x in cpsb_list if x.strip()])
            if cpsb and cpsb.strip():
                brands.insert(0, cpsb.strip())

            if not brands:
                search_configs.append({"qymc": qymc, "cpsb": None})
            else:
                for b in brands:
                    search_configs.append({"qymc": qymc, "cpsb": b})

        for pc in range(pc_from, pc_to + 1):
            for cfg in search_configs:
                c_qymc = cfg["qymc"]
                c_cpsb = cfg["cpsb"]

                if on_progress:
                    on_progress({"stage": "LIST", "pc": pc, "cpsb": c_cpsb, "qymc": c_qymc})

                items.extend(iter_cp_list(session, qymc=c_qymc, pc=pc, cpsb=c_cpsb, clxh=clxh, clmc=clmc, page_size=page_size))

                if limit > 0 and len(items) >= limit:
                    break
            if limit > 0 and len(items) >= limit:
                break

        if limit > 0:
            items = items[: limit]
        if on_progress:
            on_progress({"stage": "LIST_DONE", "count": len(items)})

    inserted = 0
    updated = 0
    skipped = 0
    failed = 0
    images_uploaded = 0
    html_docs = 0
    failed_items: list[dict[str, Any]] = []

    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        # Try to use local Chrome if available to avoid download issues
        # Common paths on macOS
        executable_path = None
        possible_paths = [
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
            "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
        ]
        
        # Check environment variable first
        if os.environ.get("CHROME_PATH"):
            possible_paths.insert(0, os.environ.get("CHROME_PATH"))
            
        for path in possible_paths:
            if os.path.exists(path):
                executable_path = path
                log("playwright.use_local_browser", path=path)
                break
        
        launch_args = {"headless": not headful}
        if executable_path:
            launch_args["executable_path"] = executable_path
            # When using executable_path, we must use launch_persistent_context or ensure channel is None
            # Actually p.chromium.launch supports executable_path
        
        try:
            browser = p.chromium.launch(**launch_args)
        except Exception as e:
            log("playwright.launch.error", err=str(e), msg="Fallback to default launch")
            # Fallback to default if local launch fails (e.g. version mismatch)
            browser = p.chromium.launch(headless=not headful)

        context = browser.new_context()
        page = context.new_page()

        for idx, it in enumerate(items, start=1):
            retry_count = 0
            max_retries = 3
            last_error = None
            success = False

            while retry_count <= max_retries:
                try:
                    log("miit.cp.process.start", idx=idx, total=len(items), cpid=it.cpid, pc=it.pc, attempt=retry_count + 1)
                    if on_progress:
                        on_progress({"stage": "CHECK", "idx": idx, "total": len(items), "cpid": it.cpid, "attempt": retry_count + 1})

                    # Dedup check: lookup vehicle by productNo/productId before visiting detail page
                    # The CpListItem has 'cpid' which maps to 'productId' in our system
                    # And 'clxh' which maps to 'productNo'
                    existing_vehicle = None
                    try:
                        # cpid is 'productId' (e.g. AB662101)
                        # clxh is 'productNo' (e.g. FV6506HADEG)
                        existing_vehicle = _lookup_vehicle(session, backend, it.cpid, it.clxh)
                        
                        # Early skip optimization: if we have HTML and at least one image, skip page visit
                        if existing_vehicle:
                            existing_images = existing_vehicle.get("images") or []
                            existing_docs = existing_vehicle.get("documents") or []
                            has_html = any(d.get("docType") == "MIIT_HTML" for d in existing_docs)
                            
                            # Fast skip: assume if we have HTML and some images, it's likely complete.
                            # We can't know the exact expected image count without visiting the page,
                            # but usually if we have > 0 images and the HTML doc, it was a successful crawl.
                            if has_html and len(existing_images) > 0:
                                log("miit.cp.exists.fast_skip", cpid=it.cpid, vid=existing_vehicle.get("id"), actual_images=len(existing_images))
                                skipped += 1
                                if on_progress:
                                    on_progress({"stage": "SKIP", "idx": idx, "total": len(items), "reason": "exists_fast_skip"})
                                success = True
                                break # Break retry loop
                                
                    except Exception as e:
                        log("miit.cp.lookup.error", err=str(e))

                    log("miit.cp.detail.open", idx=idx, total=len(items), cpid=it.cpid, pc=it.pc)
                    if on_progress:
                        on_progress({"stage": "DETAIL", "idx": idx, "total": len(items), "cpid": it.cpid, "pc": it.pc, "cpsb": it.cpsb})
                    
                    page.goto(it.detail_url, wait_until="domcontentloaded", timeout=60_000)
                    _maybe_wait_for_captcha(page)
                    page.wait_for_selector("table.query_result_table", timeout=60_000)
                    html = page.content()
                    field_map, img_urls = parse_detail_html(html, it.detail_url)
                    spec = to_vehicle_spec_item(field_map, it.detail_url, html)
                    spec.setdefault("brand", it.cpsb)
                    spec.setdefault("model", it.clxh)
                    spec.setdefault("vehicleType", it.clmc)
                    
                    # Fill defaults for required fields if missing
                    if spec.get("modelYear") is None:
                        spec["modelYear"] = datetime.now().year
                    if spec.get("curbWeight") is None:
                        spec["curbWeight"] = 0 # use 0 instead of 1
                    if spec.get("fuelType") is None:
                        spec["fuelType"] = "未知"
                    if spec.get("vehicleType") is None:
                        spec["vehicleType"] = "未知"
                    if spec.get("productNo") is None and it.clxh:
                        spec["productNo"] = it.clxh
                    if spec.get("productId") is None and it.cpid:
                        spec["productId"] = it.cpid

                    if existing_vehicle:
                        existing_images = existing_vehicle.get("images") or []
                        existing_docs = existing_vehicle.get("documents") or []
                        has_html = any(d.get("docType") == "MIIT_HTML" for d in existing_docs)
                        expected_images = len(img_urls)
                        has_all_images = len(existing_images) >= expected_images
                        if has_html and has_all_images:
                            log("miit.cp.exists.skip", cpid=it.cpid, vid=existing_vehicle.get("id"), expected_images=expected_images, actual_images=len(existing_images))
                            skipped += 1
                            if on_progress:
                                on_progress({"stage": "SKIP", "idx": idx, "total": len(items), "reason": "exists_complete"})
                            success = True
                            break # Break retry loop
                        log("miit.cp.exists.incomplete", cpid=it.cpid, vid=existing_vehicle.get("id"), expected_images=expected_images, actual_images=len(existing_images), has_html=has_html)

                    r1 = upsert_vehicle_specs(session, backend, [spec], run_id=None)
                    inserted_delta = int(r1.get("inserted", 0))
                    updated_delta = int(r1.get("updated", 0))
                    
                    if on_progress:
                        on_progress({"stage": "UPSERT_SPEC", "inserted": inserted, "updated": updated, "skipped": skipped, "failed": failed})

                    vehicle = _lookup_vehicle(session, backend, spec.get("productId"), spec.get("productNo"))
                    vehicle_id = int(vehicle["id"]) if vehicle and vehicle.get("id") else None
                    if vehicle_id is None:
                        raise RuntimeError(f"vehicle id not found after upsert: cpid={it.cpid}")
                    img_map = {}
                    try:
                        expected_image_count = len(img_urls)
                        img_count, img_map = _download_and_upload_images(session, backend, context, vehicle_id, it, img_urls)
                        if img_count != expected_image_count:
                            raise RuntimeError(f"image incomplete for cpid={it.cpid}, expected={expected_image_count}, uploaded={img_count}")
                        images_uploaded += img_count
                        html_docs += _upload_html_doc(session, backend, vehicle_id, it, _rewrite_html(html, img_map))
                        inserted += inserted_delta
                        updated += updated_delta
                    except Exception as e:
                        _delete_vehicle(session, backend, vehicle_id)
                        raise RuntimeError(f"vehicle rolled back for cpid={it.cpid}: {str(e)}")
                    
                    if on_progress:
                        on_progress({"stage": "DETAIL_DONE", "idx": idx, "total": len(items), "inserted": inserted, "updated": updated, "skipped": skipped, "failed": failed, "imagesUploaded": images_uploaded, "htmlDocs": html_docs})
                    
                    success = True
                    break # Break retry loop

                except Exception as e:
                    last_error = e
                    log("miit.cp.detail.error", cpid=it.cpid, pc=it.pc, err=str(e), attempt=retry_count + 1)
                    retry_count += 1
                    if retry_count <= max_retries:
                        time.sleep(2) # Wait before retry
                    else:
                        # All retries failed
                        failed += 1
                        failed_items.append({
                            "cpid": it.cpid,
                            "pc": it.pc,
                            "dataTag": it.data_tag,
                            "qymc": it.qymc,
                            "cpsb": it.cpsb,
                            "clxh": it.clxh,
                            "clmc": it.clmc,
                            "error": str(e)
                        })
                        if on_progress:
                            on_progress({"stage": "DETAIL_ERROR", "idx": idx, "total": len(items), "cpid": it.cpid, "pc": it.pc, "error": str(e), "inserted": inserted, "updated": updated, "skipped": skipped, "failed": failed})

        browser.close()

    return {
        "count": len(items),
        "inserted": inserted,
        "updated": updated,
        "skipped": skipped,
        "failed": failed,
        "html_docs": html_docs,
        "images_uploaded": images_uploaded,
        "failed_items": failed_items,
    }


def _maybe_wait_for_captcha(page) -> None:
    txt = page.content()
    if "拖动滑块" in txt or "拼图" in txt:
        log("captcha.detected.init")
        if CaptchaSolver.solve(page):
            log("captcha.auto_solved")
            return
        # If auto-solve fails, fallback to manual
        log("captcha.manual_fallback")
        input("检测到访问验证，自动识别失败，请在浏览器窗口中完成验证后回车继续...")


def _lookup_vehicle(session: requests.Session, backend: str, product_id: str | None, product_no: str | None) -> dict[str, Any] | None:
    base = backend.rstrip("/")
    if product_id:
        url = f"{base}/api/admin/vehicles/lookup?productId={quote(product_id)}"
    elif product_no:
        url = f"{base}/api/admin/vehicles/lookup?productNo={quote(product_no)}"
    else:
        return None
    resp = session.get(url, timeout=20)
    if resp.status_code == 404:
        return None
    resp.raise_for_status()
    return resp.json()


def _delete_vehicle(session: requests.Session, backend: str, vehicle_id: int) -> None:
    base = backend.rstrip("/")
    resp = session.delete(f"{base}/api/admin/vehicles/{vehicle_id}", timeout=30)
    if resp.status_code == 404:
        return
    resp.raise_for_status()


def _upload_html_doc(session: requests.Session, backend: str, vehicle_id: int | None, it: CpListItem, html: str) -> int:
    sha = hashlib.sha256(html.encode("utf-8")).hexdigest()
    name = f"miit_{it.pc}_{it.cpid}.html"
    with tempfile.NamedTemporaryFile(delete=False, suffix=".html") as f:
        f.write(html.encode("utf-8"))
        tmp_path = f.name
    try:
        path = f"vehicles/{vehicle_id}" if vehicle_id is not None else f"miit_html/{it.pc}"
        url = _upload_file(session, backend, tmp_path, path)
        doc = {
            "productId": it.cpid,
            "productNo": it.clxh,
            "docType": "MIIT_HTML",
            "docName": name,
            "docUrl": url,
            "sha256": sha,
            "sourceUrl": it.detail_url,
            "fetchedAt": datetime.now().isoformat(timespec="seconds"),
        }
        upsert_vehicle_documents(session, backend, [doc], run_id=None)
        return 1
    finally:
        try:
            os.unlink(tmp_path)
        except Exception:
            pass


def _upload_file(session: requests.Session, backend: str, file_path: str, path: str) -> str:
    base = backend.rstrip("/")
    url = f"{base}/api/admin/files/upload"
    with open(file_path, "rb") as f:
        files = {"file": (os.path.basename(file_path), f, "application/octet-stream")}
        resp = session.post(url, files=files, data={"path": path}, timeout=60)
        resp.raise_for_status()
        data = resp.json()
        return data["url"]


def _download_and_upload_images(session: requests.Session, backend: str, context, vehicle_id: int, it: CpListItem, img_urls: list[str]) -> tuple[int, dict[str, str]]:
    if not img_urls:
        return 0, {}
    uploaded = 0
    mapping: dict[str, str] = {}
    base = backend.rstrip("/")
    for i, u in enumerate(img_urls):
        retry_download = 0
        while retry_download < 3:
            try:
                # Add headers to mimic browser request
                headers = {
                    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer": "https://service.miit-eidc.org.cn/"
                }
                resp = context.request.get(u, timeout=60_000, headers=headers)
                if not resp.ok:
                    log("miit.cp.image.download.failed", url=u, status=resp.status, attempt=retry_download + 1)
                    retry_download += 1
                    time.sleep(1)
                    continue
                    
                body = resp.body()
                if not body or len(body) < 100: # Basic size check
                     log("miit.cp.image.download.empty", url=u, size=len(body) if body else 0, attempt=retry_download + 1)
                     retry_download += 1
                     time.sleep(1)
                     continue

                with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as f:
                    f.write(body)
                    tmp_path = f.name
                try:
                    with open(tmp_path, "rb") as fp:
                        files = {"file": (f"miit_{it.pc}_{it.cpid}_{i}.jpg", fp, "image/jpeg")}
                        r = session.post(
                            f"{base}/api/admin/vehicles/{vehicle_id}/images",
                            files=files,
                            data={"name": f"miit_{it.pc}_{it.cpid}_{i}", "sort": str(i)},
                            timeout=60,
                        )
                        r.raise_for_status()
                        data = r.json()
                        if isinstance(data, dict) and data.get("imageUrl"):
                            mapping[u] = data["imageUrl"]
                            uploaded += 1
                            break # Success, break retry loop
                finally:
                    try:
                        os.unlink(tmp_path)
                    except Exception:
                        pass
            except Exception as e:
                log("miit.cp.image.download.error", url=u, err=str(e), attempt=retry_download + 1)
                retry_download += 1
                time.sleep(1)
                continue
    return uploaded, mapping


def _rewrite_html(html: str, img_map: dict[str, str]) -> str:
    if not img_map:
        return html
    try:
        from bs4 import BeautifulSoup
        soup = BeautifulSoup(html, "html.parser")
        imgs = soup.find_all("img")
        
        # Base URL for resolving relative paths
        base_url = "https://service.miit-eidc.org.cn/miitxxgk/gonggao/xxgk/queryCpData"
        
        changed = False
        
        # Remove external CSS to avoid 404/MIME errors
        for link in soup.find_all("link", rel="stylesheet"):
            link.decompose()
            changed = True

        # Inject basic table styles
        style = soup.new_tag("style")
        style.string = """
            body { font-family: Arial, sans-serif; font-size: 14px; }
            table { border-collapse: collapse; width: 100%; margin-bottom: 10px; background-color: #fff; }
            td, th { border: 1px solid #ccc; padding: 6px 10px; text-align: left; }
            td.title { background-color: #f0f5f9; font-weight: bold; width: 150px; }
            img { max-width: 100%; height: auto; display: block; margin: 5px 0; }
        """
        if soup.head:
            soup.head.append(style)
            changed = True
        elif soup.body:
            soup.body.insert(0, style)
            changed = True

        for img in imgs:
            src = img.get("src")
            if not src:
                continue
            
            # Normalize src to absolute URL to match with img_map keys
            abs_src = requests.compat.urljoin(base_url, src)
            
            # Try exact match
            if abs_src in img_map:
                img["src"] = img_map[abs_src]
                changed = True
                continue
                
            # Try fuzzy match (sometimes urljoin might be slightly off due to query params order)
            # But here we rely on the map built during download which used the same extraction logic
            
            # Also try to replace in the map if the key in map is relative (unlikely but possible)
            if src in img_map:
                img["src"] = img_map[src]
                changed = True
        
        if changed:
            return str(soup)
        return html
    except Exception as e:
        log("html.rewrite.error", err=str(e))
        return html
