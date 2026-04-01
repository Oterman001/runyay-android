"""
笔记详情操作：跑步内容检测、点赞、收藏。

设计原则：
  - 多策略检测底部操作栏按钮（content-desc → 坐标区间 → text fallback）
  - 操作后验证状态变化（防误判）
  - 所有点击随机偏移 ±8px，模拟真人
"""

from __future__ import annotations

import random
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from typing import Optional

import uiautomator2 as u2
from loguru import logger

from anti_detection.human_delay import HumanDelay

# ── 跑步相关关键词（中文 + 英文，全小写匹配） ──────────────────────────────
_RUNNING_KEYWORDS: set[str] = {
    # 活动类型
    "跑步", "跑者", "跑友", "跑团", "跑圈", "晨跑", "夜跑", "慢跑", "长跑",
    "越野跑", "trail", "越野", "山地跑", "路跑",
    # 赛事
    "马拉松", "半马", "全马", "10km", "5km", "21km", "42km",
    "完赛", "pb", "完赛奖牌", "赛事", "比赛", "报名",
    # 装备
    "跑鞋", "跑步机", "garmin", "佳明", "coros", "高驰",
    "nike run", "nrc", "adidas", "亚瑟士", "asics",
    "心率", "步频", "步幅", "gps手表",
    # 训练指标
    "配速", "跑量", "公里", "分钟/公里", "min/km", "km/h",
    "有氧", "无氧", "乳酸阈", "最大摄氧量", "vo2max",
    # 打卡类
    "跑步打卡", "打卡跑步", "运动打卡", "跑步日记", "跑步记录",
    "今日跑量", "今天跑了",
}


@dataclass
class ActionBarState:
    """底部操作栏按钮状态。"""
    liked: bool = False
    collected: bool = False
    like_cx: int = 0
    like_cy: int = 0
    collect_cx: int = 0
    collect_cy: int = 0
    like_found: bool = False
    collect_found: bool = False


class NoteAction:
    """
    笔记详情页交互类。

    职责：
      1. 判断笔记是否跑步相关（is_running_note）
      2. 解析底部操作栏状态（get_action_bar_state）
      3. 执行点赞 / 收藏并验证结果（tap_like / tap_collect）
    """

    def __init__(self, d: u2.Device, delay: HumanDelay):
        self.d = d
        self.delay = delay

    # ------------------------------------------------------------------ #
    # 内容判断
    # ------------------------------------------------------------------ #

    def is_running_note(self) -> bool:
        """
        检测当前笔记页面是否包含跑步相关内容。

        策略：
          1. dump_hierarchy 获取全页面文本（text + content-desc）
          2. 合并所有文本后与关键词集合进行 substring 匹配
          3. 任意一个关键词命中即返回 True

        注意：dump 耗时约 0.3-0.8s，已在调用处管理延迟节奏。
        """
        try:
            xml_str = self.d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            fragments: list[str] = []
            for node in root.iter():
                for attr in ("text", "content-desc"):
                    v = (node.get(attr) or "").strip()
                    if v:
                        fragments.append(v.lower())
            combined = " ".join(fragments)
            for kw in _RUNNING_KEYWORDS:
                if kw in combined:
                    logger.debug(f"命中跑步关键词: {kw!r}")
                    return True
            logger.debug("未命中跑步关键词")
        except Exception as e:
            logger.debug(f"is_running_note 解析失败: {e}")
        return False

    # ------------------------------------------------------------------ #
    # 操作栏状态解析
    # ------------------------------------------------------------------ #

    def get_action_bar_state(self) -> ActionBarState:
        """
        解析笔记详情底部操作栏中点赞/收藏按钮的位置与状态。

        XHS 操作栏识别规则（实机观察）：
          - 整体位于屏幕下方约 15-20% 区域（bar_top = screen_h * 0.80）
          - 点赞按钮：content-desc 含 "赞"，位于操作栏右侧（x > 600）
            已点赞：content-desc 含 "已赞" 或 "取消赞"
          - 收藏按钮：content-desc 含 "收藏"
            已收藏：content-desc 含 "已收藏" 或 "取消收藏"

        多重 fallback：
          1. content-desc 匹配（最可靠）
          2. text 属性匹配（部分版本使用 text）
          3. 若均失败，返回空坐标（调用方判断 like_found=False）
        """
        state = ActionBarState()
        try:
            xml_str = self.d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            screen_h = self.d.info.get("displayHeight", 2400)
            screen_w = self.d.info.get("displayWidth", 1080)
            bar_top = int(screen_h * 0.78)   # 操作栏起始 y（屏幕下 22%）

            candidates: list[tuple[int, int, int, int, str, str]] = []  # (l,top,r,bot,cd,text)

            for node in root.iter():
                bounds = node.get("bounds", "")
                if not bounds:
                    continue
                try:
                    parts = bounds.replace("][", ",").strip("[]").split(",")
                    l, top, r, bot = (int(p) for p in parts)
                except Exception:
                    continue
                # 仅保留底部操作栏范围内的节点
                if top < bar_top:
                    continue
                cd = (node.get("content-desc") or "").strip()
                txt = (node.get("text") or "").strip()
                if cd or txt:
                    candidates.append((l, top, r, bot, cd, txt))

            # ── 从候选节点中识别点赞 / 收藏按钮 ──────────────────────────
            for l, top, r, bot, cd, txt in candidates:
                cx = (l + r) // 2
                cy = (top + bot) // 2
                label = cd or txt   # 优先 content-desc

                # 点赞按钮（"赞" 关键词，排除"收藏"）
                if "赞" in label and "收藏" not in label and not state.like_found:
                    state.like_found = True
                    state.like_cx = cx
                    state.like_cy = cy
                    # 已点赞状态判断
                    state.liked = any(p in label for p in ("已赞", "取消赞", "已点赞"))
                    logger.debug(f"点赞按钮: ({cx},{cy}) cd={label[:30]!r} liked={state.liked}")

                # 收藏按钮
                if "收藏" in label and not state.collect_found:
                    state.collect_found = True
                    state.collect_cx = cx
                    state.collect_cy = cy
                    state.collected = any(p in label for p in ("已收藏", "取消收藏"))
                    logger.debug(f"收藏按钮: ({cx},{cy}) cd={label[:30]!r} collected={state.collected}")

            # ── Fallback：若未找到，按位置推断（操作栏右侧区域） ──────────
            if not state.like_found:
                logger.debug("未通过 cd/text 找到点赞按钮，尝试位置推断")
                state = self._infer_buttons_by_position(state, screen_w, screen_h)

        except Exception as e:
            logger.warning(f"get_action_bar_state 解析异常: {e}")
        return state

    def _infer_buttons_by_position(
        self, state: ActionBarState, screen_w: int, screen_h: int
    ) -> ActionBarState:
        """
        当 content-desc/text 均无法定位按钮时，按 XHS 实测布局推断坐标。

        XHS NoteDetailActivity 底部操作栏（实机测量，1080x2373）：
          [评论输入框] [点赞按钮 x≈55%W] [收藏按钮 x≈73%W] [评论数 x≈92%W]
          y 坐标约为 screen_h - 130（即倒数第 130px 处）

        换算比例（基于实测 cx/screen_w）：
          点赞:  x = screen_w * 0.55,  y = screen_h - 130
          收藏:  x = screen_w * 0.73,  y = screen_h - 130
        """
        bar_y = screen_h - 130
        state.like_cx = int(screen_w * 0.55)
        state.like_cy = bar_y
        state.like_found = True
        state.collect_cx = int(screen_w * 0.73)
        state.collect_cy = bar_y
        state.collect_found = True
        logger.debug(f"位置推断: 点赞({state.like_cx},{state.like_cy}) 收藏({state.collect_cx},{state.collect_cy})")
        return state

    # ------------------------------------------------------------------ #
    # 点赞
    # ------------------------------------------------------------------ #

    def tap_like(self) -> bool:
        """
        对当前笔记执行点赞。

        流程：
          1. 读取操作栏状态
          2. 若已点赞则跳过
          3. 随机偏移 ±8px 点击，等待 0.8-1.5s
          4. 重新读取状态确认点赞成功

        Returns:
            True 表示本次成功点赞（之前未赞 → 现在已赞）。
        """
        state = self.get_action_bar_state()
        if state.liked:
            logger.info("笔记已点赞，跳过")
            return False
        if not state.like_found:
            logger.warning("未找到点赞按钮，跳过")
            return False

        cx, cy = state.like_cx, state.like_cy
        off_x = random.randint(-8, 8)
        off_y = random.randint(-6, 6)
        logger.info(f"点赞 → ({cx + off_x}, {cy + off_y})")
        self.d.click(cx + off_x, cy + off_y)
        time.sleep(random.uniform(0.8, 1.5))

        # 验证
        new_state = self.get_action_bar_state()
        success = new_state.liked
        if success:
            logger.info("点赞成功 ✓")
        else:
            logger.warning("点赞后状态未变化（可能失败）")
        return success

    # ------------------------------------------------------------------ #
    # 收藏
    # ------------------------------------------------------------------ #

    def tap_collect(self) -> bool:
        """
        对当前笔记执行收藏。

        Returns:
            True 表示本次成功收藏。
        """
        state = self.get_action_bar_state()
        if state.collected:
            logger.info("笔记已收藏，跳过")
            return False
        if not state.collect_found:
            logger.warning("未找到收藏按钮，跳过")
            return False

        cx, cy = state.collect_cx, state.collect_cy
        off_x = random.randint(-8, 8)
        off_y = random.randint(-6, 6)
        logger.info(f"收藏 → ({cx + off_x}, {cy + off_y})")
        self.d.click(cx + off_x, cy + off_y)
        time.sleep(random.uniform(0.8, 1.5))

        new_state = self.get_action_bar_state()
        success = new_state.collected
        if success:
            logger.info("收藏成功 ✓")
        else:
            logger.warning("收藏后状态未变化（可能失败）")
        return success
