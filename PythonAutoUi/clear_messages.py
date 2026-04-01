"""
清理小红书未读消息 — 独立入口

用法：
  python clear_messages.py              # 清理所有未读消息
  python clear_messages.py --limit 10   # 最多清理 10 条
  python clear_messages.py --serial xxxx
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from loguru import logger
from rich.console import Console

import utils.logger  # 初始化日志
from core.device import Device
from core.navigator import Navigator
from utils.config_loader import load_config

console = Console()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="小红书未读消息清理脚本")
    parser.add_argument("--limit", type=int, default=0, help="最多清理条数（0 = 不限）")
    parser.add_argument("--serial", type=str, default="", help="设备序列号")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    config = load_config()
    if args.serial:
        config.device.serial = args.serial

    console.print("\n[bold green]小红书未读消息清理[/bold green]")

    device = Device(serial=config.device.serial)
    try:
        device.connect()
        device.launch_xhs()
        time.sleep(2.0)

        nav = Navigator(device.d)

        if args.limit > 0:
            # 带限额：逐条清理，达到限额即停止
            console.print(f"目标：清理最多 [bold]{args.limit}[/bold] 条未读消息")
            cleared = _clear_with_limit(nav, device.d, args.limit)
        else:
            cleared = nav.clear_unread_messages()

        console.print(f"\n[bold cyan]清理完成，共清除 {cleared} 条未读会话[/bold cyan]")

    except Exception as e:
        logger.exception(f"清消息脚本异常: {e}")
    finally:
        device.press_home()
        logger.info("脚本退出")


def _clear_with_limit(nav: Navigator, d, limit: int) -> int:
    """
    在 clear_unread_messages 基础上加条数限制。
    通过 monkey-patch nav 内部计数器实现：每清一条检查是否达到 limit。
    这里直接用改写版本的逻辑，避免侵入 navigator.py。
    """
    import xml.etree.ElementTree as ET
    import random

    nav.go_to_messages_tab()
    time.sleep(1.0)

    cleared = 0
    consecutive_empty = 0
    max_empty_scrolls = 3
    processed_cds: set[str] = set()
    non_chat_cds: set[str] = set()

    def _find_unread_items() -> list[tuple[int, int, str]]:
        items = []
        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            for node in root.iter():
                if "ViewGroup" not in node.get("class", ""):
                    continue
                cd = (node.get("content-desc") or "").strip()
                if "未读" not in cd or cd in non_chat_cds:
                    continue
                bounds = node.get("bounds", "")
                if not bounds:
                    continue
                parts = bounds.replace("][", ",").strip("[]").split(",")
                l, top, r, bot = (int(p) for p in parts)
                if top > 650 and (r - l) > 800:
                    items.append((top, (l + r) // 2, (top + bot) // 2, cd))
        except Exception as e:
            logger.debug(f"扫描未读项失败: {e}")
        items.sort(key=lambda x: x[0])
        return [(cx, cy, cd) for _, cx, cy, cd in items]

    while consecutive_empty < max_empty_scrolls and cleared < limit:
        unread_items = [
            (cx, cy, cd) for cx, cy, cd in _find_unread_items()
            if cd not in processed_cds
        ]

        if not unread_items:
            consecutive_empty += 1
            logger.debug(f"当前屏幕无新的未读会话（连续 {consecutive_empty} 屏）")
            if consecutive_empty < max_empty_scrolls:
                screen_w = d.info.get("displayWidth", 1080)
                screen_h = d.info.get("displayHeight", 2400)
                d.swipe(
                    screen_w // 2, int(screen_h * 0.70),
                    screen_w // 2, int(screen_h * 0.30),
                    duration=0.20,
                )
                time.sleep(1.0)
            continue

        consecutive_empty = 0

        for cx, cy, cd in unread_items:
            if cleared >= limit:
                logger.info(f"已达目标条数 {limit}，停止清理")
                return cleared

            processed_cds.add(cd)
            logger.info(f"点开未读会话 ({cleared+1}/{limit}): {cd[:30]!r}")
            d.click(cx, cy)

            entered = False
            for _ in range(8):
                time.sleep(0.4)
                if "ChatActivity" in nav.current_activity():
                    entered = True
                    break

            if entered:
                time.sleep(random.uniform(0.5, 1.0))
                d.press("back")
                time.sleep(1.0)
                cleared += 1
                logger.info(f"已清除（{cleared}/{limit}）")
            else:
                non_chat_cds.add(cd)
                logger.debug(f"非聊天会话，跳过: {cd[:30]!r}")
                d.press("back")
                time.sleep(0.5)

    logger.info(f"未读消息清理完成，共清除 {cleared} 条")
    return cleared


if __name__ == "__main__":
    main()
