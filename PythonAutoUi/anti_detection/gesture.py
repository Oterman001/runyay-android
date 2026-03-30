"""
拟人化手势：坐标随机偏移的点击和滑动。
"""

from __future__ import annotations

import random

import uiautomator2 as u2
from loguru import logger


def human_tap(d: u2.Device, element) -> None:
    """
    对 uiautomator2 element 进行随机偏移点击。
    偏移范围：中心点 ±15px（水平）/ ±10px（垂直）。
    """
    try:
        bounds = element.info["bounds"]
        cx = (bounds["left"] + bounds["right"]) // 2
        cy = (bounds["top"] + bounds["bottom"]) // 2
        cx += random.randint(-15, 15)
        cy += random.randint(-10, 10)
        d.click(cx, cy)
    except Exception:
        # fallback：直接点击 element
        element.click()


def human_swipe_up(d: u2.Device, distance: int = 1200) -> None:
    """
    向上滑动（翻下一页），起止坐标加随机抖动。

    Args:
        distance: 滑动距离（像素），默认覆盖约半屏。
    """
    screen_w = d.info.get("displayWidth", 1080)
    screen_h = d.info.get("displayHeight", 2400)

    x = screen_w // 2 + random.randint(-40, 40)
    start_y = int(screen_h * 0.75) + random.randint(-80, 80)
    end_y = start_y - distance + random.randint(-100, 100)
    end_y = max(50, end_y)  # 不要滑出屏幕顶部

    duration = random.uniform(0.3, 0.7)
    d.swipe(x, start_y, x, end_y, duration=duration)
    logger.debug(f"滑动: ({x},{start_y}) → ({x},{end_y}) dur={duration:.2f}s")


def human_swipe_down(d: u2.Device, distance: int = 800) -> None:
    """向下滑动（返回列表顶部或刷新）。"""
    screen_w = d.info.get("displayWidth", 1080)
    screen_h = d.info.get("displayHeight", 2400)

    x = screen_w // 2 + random.randint(-40, 40)
    start_y = int(screen_h * 0.25) + random.randint(-80, 80)
    end_y = start_y + distance + random.randint(-100, 100)
    end_y = min(screen_h - 50, end_y)

    duration = random.uniform(0.3, 0.6)
    d.swipe(x, start_y, x, end_y, duration=duration)
