"""
主页解析器（v2）：基于实机验证的 content-desc 解析方案。

实机验证结果（XHS 9.x, Android 16）：
  - 关注数：Button content-desc = "NNN关注"（如"8387关注"）
  - 粉丝数：Button content-desc = "NNN粉丝"（如"7050粉丝"）
  - 关注按钮：Button text = "关注" / "已关注" / "互相关注"
  - 蓝V认证：content-desc 含"认证" 或 "官方"
"""

from __future__ import annotations

import re
import time
from typing import Optional

import uiautomator2 as u2
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_fixed

from persistence.models import BloggerProfile
from utils.cn_number import parse_cn_number

# 关注按钮文字
_MUTUAL_TEXTS = {"互相关注"}
_FOLLOWED_TEXTS = {"已关注", "互相关注"}
_FOLLOWABLE_TEXTS = {"关注", "+ 关注", "+关注"}


class ProfileParser:
    def __init__(self, d: u2.Device):
        self._d = d

    def parse(self, username: str) -> Optional[BloggerProfile]:
        """
        解析当前显示的用户主页。
        调用前须已通过点击 item 进入该用户主页。

        Returns:
            BloggerProfile，或 None（加载失败）。
        """
        if not self._wait_for_stats(timeout=8):
            logger.warning(f"主页统计数据加载超时: {username}")
            return None

        profile = BloggerProfile(username=username)
        profile.following = self._extract_following()
        profile.followers = self._extract_followers()
        profile.notes_count = self._extract_notes_count()
        follow_state = self._get_follow_state()
        profile.is_already_followed = follow_state in ("followed", "mutual")
        profile.is_mutual_follow = follow_state == "mutual"
        profile.is_verified = self._is_verified()

        logger.info(
            f"[主页] {username} | "
            f"粉丝={profile.followers} 关注={profile.following} "
            f"笔记={profile.notes_count} 认证={profile.is_verified} "
            f"状态={follow_state}"
        )
        return profile

    # ------------------------------------------------------------------ #
    # 等待加载
    # ------------------------------------------------------------------ #

    def _wait_for_stats(self, timeout: int = 8) -> bool:
        """等待粉丝统计 Button 出现（主页已加载的信号）。"""
        return self._d(
            descriptionContains="粉丝", className="android.widget.Button"
        ).wait(timeout=timeout)

    # ------------------------------------------------------------------ #
    # 数值解析（content-desc 方案）
    # ------------------------------------------------------------------ #

    def _extract_following(self) -> int:
        """
        从 content-desc="NNN关注" 的 Button 提取关注数。
        注意：排除 text="关注"/"已关注" 的操作按钮。
        """
        d = self._d
        try:
            for btn in d(className="android.widget.Button"):
                cd = btn.info.get("contentDescription", "") or ""
                if cd.endswith("关注") and not cd.startswith("关注"):
                    num_str = cd[: cd.rfind("关注")].strip()
                    return parse_cn_number(num_str)
        except Exception as e:
            logger.debug(f"提取关注数失败: {e}")
        return 0

    def _extract_followers(self) -> int:
        """从 content-desc="NNN粉丝" 的 Button 提取粉丝数。"""
        d = self._d
        try:
            for btn in d(className="android.widget.Button"):
                cd = btn.info.get("contentDescription", "") or ""
                if cd.endswith("粉丝"):
                    num_str = cd[: cd.rfind("粉丝")].strip()
                    return parse_cn_number(num_str)
        except Exception as e:
            logger.debug(f"提取粉丝数失败: {e}")
        return 0

    def _extract_notes_count(self) -> int:
        """
        提取笔记数。
        XHS 在主页 Tab 上不直接显示数量，尝试多种方案：
          1. content-desc 含"笔记"且有数字
          2. XPath 找笔记 Tab 附近数字
          3. 默认返回 1（有笔记内容即视为活跃账号）
        """
        d = self._d
        try:
            # 方案1：content-desc 模式
            for btn in d(className="android.widget.Button"):
                cd = btn.info.get("contentDescription", "") or ""
                if "笔记" in cd:
                    nums = re.findall(r"\d+", cd)
                    if nums:
                        return int(nums[0])

            # 方案2：ViewGroup Tab 的 text
            for vg in d(className="android.view.ViewGroup"):
                cd = vg.info.get("contentDescription", "") or ""
                if "笔记" in cd:
                    nums = re.findall(r"\d+", cd)
                    if nums:
                        return int(nums[0])

            # 方案3：主页有内容，给一个基础值让评分算法正常运作
            # 若页面有 RecyclerView（笔记列表），说明有内容
            if d(className="androidx.recyclerview.widget.RecyclerView").exists:
                return 10  # 给保守默认值
        except Exception as e:
            logger.debug(f"提取笔记数失败: {e}")

        return 0

    # ------------------------------------------------------------------ #
    # 关注状态检测
    # ------------------------------------------------------------------ #

    def _get_follow_state(self) -> str:
        """
        检测关注按钮当前状态。
        关注按钮特征：text=关注/已关注/互相关注，宽度约 200-300px，位于右侧。

        Returns:
            'not_followed' | 'followed' | 'mutual' | 'unknown'
        """
        d = self._d
        # 按优先级检测
        for text in _MUTUAL_TEXTS:
            if d(text=text, className="android.widget.Button").exists:
                return "mutual"
        for text in _FOLLOWED_TEXTS - _MUTUAL_TEXTS:
            if d(text=text, className="android.widget.Button").exists:
                return "followed"
        for text in _FOLLOWABLE_TEXTS:
            if d(text=text, className="android.widget.Button").exists:
                return "not_followed"
        return "unknown"

    # ------------------------------------------------------------------ #
    # 认证检测
    # ------------------------------------------------------------------ #

    def _is_verified(self) -> bool:
        """检测蓝V / 官方认证标志。
        注意：uiautomator2 v3 的 .exists 返回 Exists 对象而非 bool，需显式转换。
        """
        d = self._d
        return bool(
            d(descriptionContains="认证").exists
            or d(descriptionContains="官方").exists
            or d(textContains="官方认证").exists
        )
