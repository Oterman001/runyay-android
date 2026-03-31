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
import xml.etree.ElementTree as ET
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
        bio, social = self._extract_bio_and_social_proof()
        profile.bio_text = bio
        profile.social_proof_text = social

        logger.info(
            f"[主页] {username} | "
            f"粉丝={profile.followers} 关注={profile.following} "
            f"笔记={profile.notes_count} 认证={profile.is_verified} "
            f"状态={follow_state}"
        )
        if profile.bio_text:
            logger.debug(f"[简介] {profile.bio_text[:60]!r}")
        return profile

    # ------------------------------------------------------------------ #
    # 等待加载
    # ------------------------------------------------------------------ #

    def _wait_for_stats(self, timeout: int = 8) -> bool:
        """
        等待主页统计区域加载完成。
        等待条件：关注/已关注/互相关注 Button 出现（uniquely 在博主主页存在）。
        不使用 text='粉丝' 因为推荐列表顶部的"粉丝" Tab 会立即误判。
        """
        d = self._d
        for text in ("关注", "已关注", "互相关注"):
            if d(text=text, className="android.widget.Button").wait(timeout=timeout):
                return True
        return False

    # ------------------------------------------------------------------ #
    # 数值解析（content-desc 方案）
    # ------------------------------------------------------------------ #

    def _extract_following(self) -> int:
        """
        提取关注数。
        方案1：contentDescription="NNN关注" Button（部分 XHS 版本）
        方案2：统计区结构为空文本 Button，子 TextView 依次是数字和标签，
               找 text='关注' 标签的前一个兄弟 TextView 里的数字。
        """
        d = self._d
        # 方案1：content-desc 模式
        try:
            for btn in d(className="android.widget.Button"):
                cd = btn.info.get("contentDescription", "") or ""
                if cd.endswith("关注") and not cd.startswith("关注"):
                    num_str = cd[: cd.rfind("关注")].strip()
                    return parse_cn_number(num_str)
        except Exception as e:
            logger.debug(f"提取关注数(cd方案)失败: {e}")

        # 方案2：XML 兄弟节点方案
        try:
            return self._extract_stat_by_label("关注")
        except Exception as e:
            logger.debug(f"提取关注数(xml方案)失败: {e}")
        return 0

    def _extract_followers(self) -> int:
        """
        提取粉丝数。
        同 _extract_following，优先 contentDescription，再用 XML 兄弟节点。
        """
        d = self._d
        # 方案1：content-desc 模式
        try:
            for btn in d(className="android.widget.Button"):
                cd = btn.info.get("contentDescription", "") or ""
                if cd.endswith("粉丝"):
                    num_str = cd[: cd.rfind("粉丝")].strip()
                    return parse_cn_number(num_str)
        except Exception as e:
            logger.debug(f"提取粉丝数(cd方案)失败: {e}")

        # 方案2：XML 兄弟节点方案
        try:
            return self._extract_stat_by_label("粉丝")
        except Exception as e:
            logger.debug(f"提取粉丝数(xml方案)失败: {e}")
        return 0

    def _extract_stat_by_label(self, label: str) -> int:
        """
        通用：在 XML 树中找 text=label 的 TextView，
        然后找同一父节点中排在它前面的 TextView（数字节点），解析数值。
        统计区结构：Button > [TextView(数字), TextView(标签)]
        """
        xml_str = self._d.dump_hierarchy()
        root = ET.fromstring(xml_str)
        for parent in root.iter():
            children = list(parent)
            for i, child in enumerate(children):
                if child.get("text", "").strip() == label and "TextView" in child.get("class", ""):
                    # 找前一个兄弟中的数字节点
                    for j in range(i - 1, -1, -1):
                        num_text = children[j].get("text", "").strip()
                        if num_text and re.search(r"\d", num_text):
                            return parse_cn_number(num_text)
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

    # ------------------------------------------------------------------ #
    # 简介 & 社交证明提取
    # ------------------------------------------------------------------ #

    def _extract_bio_and_social_proof(self) -> tuple[str, str]:
        """
        一次 dump_hierarchy 同时提取：
          - 简介 (bio)：全宽 clickable TextView（宽度 > 700px，y 400-900）
          - 社交证明：含"关注了他/她"的文字（关注者昵称，含互关圈信号）
        返回 (bio_text, social_proof_text)。
        """
        bio = ""
        social = ""
        try:
            xml_str = self._d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            for node in root.iter():
                t = (node.get("text", "") or "").strip()
                if not t:
                    continue
                cls = node.get("class", "")
                b = node.get("bounds", "")
                if not b:
                    continue

                # ── 社交证明：含"关注了他/她" ──────────────────────────────
                if ("关注了他" in t or "关注了她" in t) and len(t) > 5:
                    social = t

                # ── 简介：全宽 clickable TextView，y=400-900 ──────────────
                if "TextView" in cls and node.get("clickable", "false") == "true":
                    try:
                        l, top, r, _ = [int(x) for x in
                                        b.replace("][", ",").strip("[]").split(",")]
                        if (r - l) > 700 and 400 < top < 900:
                            if len(t) > len(bio):
                                bio = t
                    except Exception:
                        pass
        except Exception as e:
            logger.debug(f"提取简介/社交证明失败: {e}")
        return bio, social
