#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
工信部 (miit-eidc) 拼图滑块验证码自动化解决方案演示脚本
此脚本演示如何使用 Playwright 模拟浏览器操作，结合 OpenCV (cv2) 识别缺口位置，
实现滑块验证码的自动拖拽。

依赖项:
    pip install playwright opencv-python-headless numpy requests

首次使用 Playwright 需要安装浏览器:
    playwright install chromium
"""

import time
import random
import requests
import numpy as np
import cv2
from playwright.sync_api import sync_playwright, Page, Locator

class MiitSliderSolver:
    def __init__(self, headless: bool = False):
        """
        初始化滑块识别器
        :param headless: 是否使用无头模式（不显示浏览器界面）
        """
        self.headless = headless

    def _get_image_content(self, url: str) -> bytes:
        """下载图片内容"""
        try:
            resp = requests.get(url, timeout=10)
            if resp.status_code == 200:
                return resp.content
        except Exception as e:
            print(f"[错误] 图片下载失败: {e}")
        return None

    def _identify_gap(self, bg_bytes: bytes, slider_bytes: bytes, out_path: str = "debug_match.jpg") -> int:
        """
        使用 OpenCV 识别滑块缺口位置
        :param bg_bytes: 背景图片二进制数据
        :param slider_bytes: 滑块图片二进制数据
        :return: 缺口左边缘的 X 坐标
        """
        # 1. 读取图片
        bg_arr = np.frombuffer(bg_bytes, np.uint8)
        slider_arr = np.frombuffer(slider_bytes, np.uint8)
        
        bg_img = cv2.imdecode(bg_arr, cv2.IMREAD_COLOR)
        slider_img = cv2.imdecode(slider_arr, cv2.IMREAD_COLOR)

        # 2. 图像预处理 (灰度化 + 边缘检测)
        # 很多滑块验证码的缺口和背景颜色差异不大，使用 Canny 边缘检测效果更好
        bg_gray = cv2.cvtColor(bg_img, cv2.COLOR_BGR2GRAY)
        slider_gray = cv2.cvtColor(slider_img, cv2.COLOR_BGR2GRAY)

        # 高斯模糊去噪
        bg_gray = cv2.GaussianBlur(bg_gray, (5, 5), 0)
        slider_gray = cv2.GaussianBlur(slider_gray, (5, 5), 0)

        # Canny 边缘检测
        bg_edge = cv2.Canny(bg_gray, 100, 200)
        slider_edge = cv2.Canny(slider_gray, 100, 200)

        # 3. 模板匹配
        # 使用 TM_CCOEFF_NORMED 相关系数匹配
        res = cv2.matchTemplate(bg_edge, slider_edge, cv2.TM_CCOEFF_NORMED)
        
        # 4. 获取最佳匹配位置
        min_val, max_val, min_loc, max_loc = cv2.minMaxLoc(res)
        
        # max_loc 是 (x, y)，即缺口左上角坐标
        x_offset = max_loc[0]

        # 调试：画出矩形框保存图片查看效果
        h, w = slider_edge.shape[:2]
        cv2.rectangle(bg_img, max_loc, (max_loc[0] + w, max_loc[1] + h), (0, 0, 255), 2)
        cv2.imwrite(out_path, bg_img)
        print(f"[调试] 缺口识别结果已保存至 {out_path}, 识别坐标 X: {x_offset}")

        return x_offset

    def _generate_track(self, distance: int) -> list[int]:
        """
        生成拟人化的滑动轨迹 (模拟物理运动：加速 -> 减速 -> 微调)
        :param distance: 需要移动的总距离
        :return: 每次移动的相对位移列表
        """
        track = []
        current = 0
        mid = distance * 4 / 5  # 减速阈值
        t = 0.2  # 计算时间间隔
        v = 0    # 初始速度
        
        while current < distance:
            if current < mid:
                a = 2  # 加速阶段
            else:
                a = -3 # 减速阶段
            
            v0 = v
            v = v0 + a * t
            move = v0 * t + 0.5 * a * t * t
            
            # 防止过冲或后退异常
            if move < 1: move = 1
            
            current += move
            track.append(round(move))

        # 简单修正：确保总距离准确
        # 实际移动可能会有误差，这里做一个简单的末尾修正
        final_pos = sum(track)
        if final_pos != distance:
            diff = distance - final_pos
            track.append(diff)
            
        return track

    def solve(self, url: str):
        """
        主流程：打开网页 -> 等待验证码 -> 下载图片 -> 识别 -> 拖动
        """
        with sync_playwright() as p:
            # 启动浏览器
            browser = p.chromium.launch(headless=self.headless)
            context = browser.new_context(
                viewport={'width': 1280, 'height': 800},
                user_agent="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            page = context.new_page()

            try:
                print(f"正在访问: {url}")
                page.goto(url)
                
                # 模拟触发验证码的操作 (需根据实际页面修改选择器)
                # 例如：点击查询按钮
                # page.click("#query-btn") 
                
                # 等待滑块图片元素出现
                # 假设背景图选择器是 .captcha-bg, 滑块图选择器是 .captcha-slider
                # 注意：这里需要替换为工信部网站实际的 CSS 选择器
                print("等待验证码加载...")
                bg_selector = "img.yidun_bg-img"  # 网易易盾示例选择器
                slider_selector = "img.yidun_jigsaw" # 网易易盾示例选择器
                
                # 显式等待元素出现
                try:
                    page.wait_for_selector(bg_selector, timeout=10000)
                except Exception:
                    print("未检测到验证码，可能无需验证或加载超时。")
                    return

                bg_elem = page.locator(bg_selector).first
                slider_elem = page.locator(slider_selector).first
                
                # 获取图片 URL
                bg_src = bg_elem.get_attribute("src")
                slider_src = slider_elem.get_attribute("src")
                
                if not bg_src or not slider_src:
                    print("无法获取验证码图片 URL")
                    return

                print(f"背景图: {bg_src[:50]}...")
                print(f"滑块图: {slider_src[:50]}...")

                # 下载图片
                bg_bytes = self._get_image_content(bg_src)
                slider_bytes = self._get_image_content(slider_src)

                if not bg_bytes or not slider_bytes:
                    print("图片下载失败")
                    return

                # 识别缺口距离
                target_x = self._identify_gap(bg_bytes, slider_bytes)
                print(f"识别到的缺口 X 坐标: {target_x}")

                # 计算缩放比例
                # 网页上显示的宽度 vs 图片原始宽度
                # bounding_box() 返回 float 类型的 x, y, width, height
                bg_box = bg_elem.bounding_box()
                if not bg_box:
                    print("无法获取背景图元素尺寸")
                    return
                
                # 假设我们需要根据原始图片的宽度来计算缩放比
                # 这里先读取原始图片宽度
                img_np = np.frombuffer(bg_bytes, np.uint8)
                img = cv2.imdecode(img_np, cv2.IMREAD_COLOR)
                original_width = img.shape[1]
                
                scale = bg_box['width'] / original_width
                print(f"图片缩放比例: {scale:.2f} (网页宽: {bg_box['width']}, 原图宽: {original_width})")
                
                # 实际需要拖动的距离
                real_distance = int(target_x * scale)
                # 微调：有时候缺口识别的是左边缘，滑块本身有宽度，或者有一定的起始偏移量
                # 网易易盾通常滑块起始位置不是0，需要减去滑块起始偏移
                # 这里的 5 是经验值，可能需要根据实际情况调整
                real_distance -= 5 
                
                print(f"计算出的实际拖动距离: {real_distance} px")

                # 生成轨迹
                tracks = self._generate_track(real_distance)
                
                # 获取滑块的操作手柄 (knob)
                # 通常是 .yidun_slider 或者类似的元素
                knob_selector = ".yidun_slider" 
                knob = page.locator(knob_selector).first
                
                # 获取滑块中心点，模拟鼠标移动到滑块中心
                knob_box = knob.bounding_box()
                if knob_box:
                    start_x = knob_box['x'] + knob_box['width'] / 2
                    start_y = knob_box['y'] + knob_box['height'] / 2
                    
                    page.mouse.move(start_x, start_y)
                    page.mouse.down()
                    
                    # 开始拖动
                    current_x = start_x
                    for track in tracks:
                        current_x += track
                        # 加上一点随机的 y 轴抖动
                        y_offset = random.randint(-2, 2)
                        page.mouse.move(current_x, start_y + y_offset)
                        # 随机停顿，模拟人手
                        time.sleep(random.uniform(0.01, 0.03))
                    
                    # 松开鼠标
                    time.sleep(0.5)
                    page.mouse.up()
                    
                    print("拖动完成，等待验证结果...")
                    time.sleep(3)
                    
                    # 检查是否验证成功
                    # 这里的判断逻辑需根据实际页面调整，例如检查是否有 "验证成功" 的提示
                    if page.is_visible(".yidun_tips__pass") or "成功" in page.content():
                         print("验证成功！")
                    else:
                         print("验证可能失败，需要重试。")

            except Exception as e:
                print(f"发生异常: {e}")
            finally:
                # 保持浏览器打开一会以便观察
                time.sleep(5)
                browser.close()

if __name__ == "__main__":
    # 示例 URL (请替换为实际的工信部查询页面 URL)
    TARGET_URL = "https://service.miit-eidc.org.cn/miitxxgk/gonggao/xxgk/queryCpData" 
    
    solver = MiitSliderSolver(headless=False)
    solver.solve(TARGET_URL)
