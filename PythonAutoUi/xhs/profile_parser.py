"""
主页解析器：进入用户主页，提取粉丝数、关注数、笔记数、关注状态。
"""

from __future__ import annotations

import time
from typing import Optional

import uiautomator2 as u2
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_fixed

from persistence.models import BloggerProfile
from utils.cn_number import parse_cn_number


# 关注按钮的可能文本（按优先级排列）
_FOLLOW_BTN_TEXTS = ["关注", "+ 关注", "+关注"]
_FOLLOWED_BTN_TEXTS = ["已关注", "互相关注"]
_MUTUAL_TEXTS = ["互相关注"]


class ProfileParser:
    def __init__(self, d: u2.Device):
        self._d = d

    def parse(self, username: str) -> Optional[BloggerProfile]:
        """
        解析当前显示的用户主页。

        注意：调用前需已通过点击卡片进入用户主页。
        username 作为 key 传入（从搜索结果获得）。

        Returns:
            BloggerProfile 或 None（页面加载失败）。
        """
        d = self._d

        # 等待主页统计数据加载（粉丝数区域出现）
        if not self._wait_for_stats(timeout=8):
            logger.warning(f"主页统计数据加载超时: {username}")
            return None

        profile = BloggerProfile(username=username)

        # 解析关注/粉丝/笔记数
        profile.followers = self._extract_count("粉丝")
        profile.following = self._extract_count("关注")
        profile.notes_count = self._extract_notes_count()

        # 解析关注按钮状态
        follow_state = self._get_follow_state()
        profile.is_already_followed = follow_state in ("followed", "mutual")
        profile.is_mutual_follow = follow_state == "mutual"

        # 检测蓝 V 认证
        profile.is_verified = self._is_verified()

        logger.info(
            f"[主页] {username} | 粉丝={profile.followers} "
            f"关注={profile.following} 笔记={profile.notes_count} "
            f"认证={profile.is_verified} 状态={follow_state}"
        )
        return profile

    # ------------------------------------------------------------------ #
    # 内部方法
    # ------------------------------------------------------------------ #

    def _wait_for_stats(self, timeout: int = 8) -> bool:
        """等待粉丝数区域出现（判断主页已加载）。"""
        return self._d(textContains="粉丝").wait(timeout=timeout)

    def _extract_count(self, label: str) -> int:
        """
        提取"粉丝"或"关注"标签上方/旁边的数字。

        XHS 主页统计布局（竖排 or 横排）：
          [数字]  [数字]  [数字]
          关注    粉丝    获赞与收藏

        策略：找 text=label 的节点，然后找其 sibling 或 parent 中的数字。
        """
        d = self._d
        try:
            label_node = d(text=label)
            if not label_node.exists:
                return 0

            # 尝试从父容器中找数字节点
            info = label_node.info
            bounds = info.get("bounds", {})
            # 数字通常在标签正上方（cy - height）
            num_y = bounds.get("top", 0) - 10
            num_x = (bounds.get("left", 0) + bounds.get("right", 0)) // 2

            # 在 label 上方区域找纯数字节点
            sibling = d.xpath(
                f'//android.widget.TextView[@text and '
                f'string-length(@text) <= 8 and '
                f'not(contains(@text, "粉丝")) and '
                f'not(contains(@text, "关注")) and '
                f'not(contains(@text, "获赞"))]'
            )

            # fallback：解析全页 XML 定位
            return self._extract_count_from_xml(label)

        except Exception as e:
            logger.debug(f"提取 '{label}' 数值失败: {e}")
            return self._extract_count_from_xml(label)

    def _extract_count_from_xml(self, label: str) -> int:
        """通过 XML 层级精确提取标签对应的数字。"""
        import xml.etree.ElementTree as ET

        try:
            xml_str = self._d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            # 找 text=label 的节点
            label_node = None
            for node in root.iter():
                if node.get("text", "").strip() == label:
                    label_node = node
                    break

            if label_node is None:
                return 0

            # 找父节点
            parent = self._find_parent(root, label_node)
            if parent is None:
                return 0

            # 在父节点的兄弟容器中找数字（统计区域通常是三列）
            grandparent = self._find_parent(root, parent)
            if grandparent is None:
                return 0

            # 遍历 grandparent 的子节点，找与 parent 同级且包含数字的
            for sibling_container in grandparent:
                # 检查该容器是否包含 label
                labels_in = [n.get("text", "") for n in sibling_container.iter()]
                if label in labels_in:
                    # 找该容器内的纯数字/中文数字节点
                    for n in sibling_container.iter():
                        text = n.get("text", "").strip()
                        if text and text != label and self._looks_like_number(text):
                            return parse_cn_number(text)

        except Exception as e:
            logger.debug(f"XML 提取 '{label}' 失败: {e}")

        return 0

    def _extract_notes_count(self) -> int:
        """提取笔记数（'笔记 N' 或数字在'笔记'tab旁）。"""
        d = self._d
        # 常见形式："笔记 123" 或 Tab 标签 "笔记(123)"
        try:
            for node in d(textContains="笔记"):
                text = node.get_text()
                # 从文本中提取数字
                import re
                nums = re.findall(r"\d+", text)
                if nums:
                    return int(nums[0])
        except Exception:
            pass
        return 0

    def _get_follow_state(self) -> str:
        """
        检测关注按钮当前状态。

        Returns:
            'not_followed' | 'followed' | 'mutual' | 'unknown'
        """
        d = self._d
        for text in _MUTUAL_TEXTS:
            if d(text=text).exists:
                return "mutual"
        for text in _FOLLOWED_BTN_TEXTS:
            if d(text=text).exists:
                return "followed"
        for text in _FOLLOW_BTN_TEXTS:
            if d(text=text).exists:
                return "not_followed"
        return "unknown"

    def _is_verified(self) -> bool:
        """检测是否有蓝 V 认证标志。"""
        d = self._d
        # 认证标志通常是 content-desc 包含"认证"或特定图标
        return (
            d(descriptionContains="认证").exists
            or d(descriptionContains="官方").exists
            or d(textContains="官方认证").exists
        )

    @staticmethod
    def _looks_like_number(text: str) -> bool:
        """判断文本是否是数字或中文数字格式。"""
        import re
        return bool(re.match(r"^[\d.,万亿]+$", text))

    @staticmethod
    def _find_parent(root, target):
        for parent in root.iter():
            for child in parent:
                if child is target:
                    return parent
        return None
