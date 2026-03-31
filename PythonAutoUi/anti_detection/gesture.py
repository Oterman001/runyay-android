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


def human_swipe_up(d: u2.Device, distance: int = 0) -> None:
    """
    向上滑动（翻下一页），模拟真人快速上划手势。

    真人操作特征：
      - 单次滑动距离约占屏高 50-70%（大距离快速划过）
      - 持续时间短（0.10-0.25s），触发 RecyclerView fling 惯性
      - 起点在屏幕中下区域，终点在屏幕中上区域

    Args:
        distance: 滑动距离（像素）。0 表示自动按屏高 50-70% 随机取值。
    """
    screen_w = d.info.get("displayWidth", 1080)
    screen_h = d.info.get("displayHeight", 2400)

    if distance <= 0:
        distance = int(screen_h * random.uniform(0.50, 0.70))

    x = screen_w // 2 + random.randint(-30, 30)
    # 起点在屏幕 65-80% 处，终点上移 distance，但不超出可见区顶部
    start_y = int(screen_h * random.uniform(0.65, 0.80))
    end_y = max(int(screen_h * 0.10), start_y - distance)

    # 快速划动才能触发 fling，duration 控制在 0.10-0.22s
    duration = random.uniform(0.10, 0.22)
    d.swipe(x, start_y, x, end_y, duration=duration)
    logger.debug(f"上划: ({x},{start_y})→({x},{end_y}) dist={start_y-end_y}px dur={duration:.2f}s")


def human_swipe_down(d: u2.Device, distance: int = 0) -> None:
    """向下滑动（短距回看），距离约屏高 20-35%，不触发 fling。"""
    screen_w = d.info.get("displayWidth", 1080)
    screen_h = d.info.get("displayHeight", 2400)

    if distance <= 0:
        distance = int(screen_h * random.uniform(0.20, 0.35))

    x = screen_w // 2 + random.randint(-30, 30)
    start_y = int(screen_h * random.uniform(0.25, 0.40))
    end_y = min(int(screen_h * 0.85), start_y + distance)

    duration = random.uniform(0.20, 0.40)
    d.swipe(x, start_y, x, end_y, duration=duration)


def human_swipe_left(d: u2.Device, distance: int = 600) -> None:
    """向左滑动（横向推荐列表翻看更多）。"""
    screen_w = d.info.get("displayWidth", 1080)
    screen_h = d.info.get("displayHeight", 2400)

    # 在屏幕下半部分滑动（横向推荐列表通常在主页下方）
    y = int(screen_h * 0.80) + random.randint(-60, 60)
    start_x = int(screen_w * 0.75) + random.randint(-40, 40)
    end_x = start_x - distance + random.randint(-80, 80)
    end_x = max(50, end_x)

    duration = random.uniform(0.25, 0.5)
    d.swipe(start_x, y, end_x, y, duration=duration)
    logger.debug(f"左滑: ({start_x},{y}) → ({end_x},{y}) dur={duration:.2f}s")
