"""
搜索模块：在小红书搜索关键词，切换到"用户"标签，滚动收集候选用户名。
"""

from __future__ import annotations

import time
from typing import List

import uiautomator2 as u2
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_fixed

from anti_detection.gesture import human_swipe_up
from anti_detection.human_delay import HumanDelay
from core.navigator import Navigator


class Searcher:
    def __init__(self, d: u2.Device, navigator: Navigator, delay: HumanDelay):
        self._d = d
        self._nav = navigator
        self._delay = delay

    def search_users(self, keyword: str, pages: int = 6) -> List[str]:
        """
        搜索关键词并收集用户名列表。

        Args:
            keyword: 搜索关键词，如"跑步博主"
            pages:   滚动翻页次数

        Returns:
            用户名列表（去重后）
        """
        logger.info(f"开始搜索: '{keyword}'，预计翻 {pages} 页")

        self._nav.go_to_search()
        self._type_keyword(keyword)
        self._switch_to_user_tab()

        usernames: List[str] = []
        seen: set[str] = set()

        for page in range(pages):
            new_names = self._collect_visible_users()
            added = 0
            for name in new_names:
                if name not in seen:
                    seen.add(name)
                    usernames.append(name)
                    added += 1

            logger.debug(f"第 {page + 1}/{pages} 页：新增 {added} 人，累计 {len(usernames)} 人")

            if page < pages - 1:
                human_swipe_up(self._d)
                self._delay.scroll_pause()

        logger.info(f"搜索 '{keyword}' 完成，共收集 {len(usernames)} 位用户")
        return usernames

    # ------------------------------------------------------------------ #
    # 内部方法
    # ------------------------------------------------------------------ #

    def _type_keyword(self, keyword: str) -> None:
        """逐字符输入关键词（模拟手动输入）并提交搜索。"""
        d = self._d
        # 清空输入框
        edit = d(focused=True, className="android.widget.EditText")
        edit.clear_text()

        # 逐字符输入（中文输入法通常直接 set_text 更可靠）
        # 先尝试 set_text，如果 XHS 有输入拦截再改为逐字符
        edit.set_text(keyword)
        time.sleep(0.5)

        # 按回车确认搜索
        d.press("enter")
        time.sleep(2.0)  # 等待结果加载
        logger.debug(f"已输入关键词: {keyword}")

    @retry(stop=stop_after_attempt(3), wait=wait_fixed(1))
    def _switch_to_user_tab(self) -> None:
        """切换到搜索结果的"用户"标签。"""
        d = self._d
        if not d(text="用户").wait(timeout=8):
            raise RuntimeError("未找到'用户'标签页")
        d(text="用户").click()
        time.sleep(1.5)
        logger.debug("已切换到'用户'标签")

    def _collect_visible_users(self) -> List[str]:
        """
        从当前页面抓取可见用户卡片的用户名。

        小红书用户卡片结构（近似）：
          - 包含头像、用户名、粉丝数
          - 用户名是普通 TextView，粉丝数后接"粉丝"字样
        """
        d = self._d
        usernames = []

        # 获取所有包含"粉丝"文字的节点，其前兄弟节点为用户名
        # 策略：找所有带"粉丝"的文本节点，取其父容器中第一个 TextView 作为用户名
        try:
            # 截取页面 XML 用于调试（仅 DEBUG 级别）
            page_xml = d.dump_hierarchy()

            # 找所有含"粉丝"的元素，向上遍历找用户名
            fan_nodes = d(textContains="粉丝")
            count = fan_nodes.count

            for i in range(count):
                try:
                    fan_node = fan_nodes[i]
                    # 获取父容器（用户卡片根节点）
                    parent_info = fan_node.info
                    # 用户名通常在同级的第一个 TextView
                    # 使用 xpath 从"粉丝"节点找兄弟节点中的用户名
                    # 具体 xpath 需根据实际 UI 结构调整
                    bounds = parent_info.get("bounds", {})
                    cx = (bounds.get("left", 0) + bounds.get("right", 0)) // 2
                    cy = bounds.get("top", 0) - 40  # 用户名在粉丝数上方

                    # 通过坐标找用户名节点（鲁棒性更高）
                    username_node = d.xpath(
                        f'//*[@bounds and @class="android.widget.TextView"]'
                        f'[not(contains(@text, "粉丝")) and not(contains(@text, "关注"))'
                        f' and string-length(@text) > 1]'
                    )
                    # fallback：直接从 XML 解析（更可靠）
                except Exception as e:
                    logger.debug(f"解析用户卡片 {i} 失败: {e}")
                    continue

            # 更简单可靠的方式：XPath 直接匹配用户卡片结构
            usernames = self._extract_usernames_from_xml()

        except Exception as e:
            logger.warning(f"收集用户名失败: {e}")

        return usernames

    def _extract_usernames_from_xml(self) -> List[str]:
        """
        通过解析页面层级 XML 提取用户名。
        小红书搜索结果中，用户卡片内用户名紧邻头像，
        在"粉丝"文字所在行的上方。
        """
        import xml.etree.ElementTree as ET

        usernames = []
        try:
            xml_str = self._d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            # 找所有 text 包含"粉丝"的节点
            for node in root.iter():
                text = node.get("text", "")
                if "粉丝" in text and len(text) < 20:
                    # 找其父节点的所有子节点中的用户名
                    # （在 XML 中向上遍历需要额外处理，这里用兄弟节点策略）
                    parent = self._find_parent(root, node)
                    if parent is not None:
                        for sibling in parent:
                            sib_text = sibling.get("text", "").strip()
                            if (
                                sib_text
                                and "粉丝" not in sib_text
                                and "关注" not in sib_text
                                and len(sib_text) >= 2
                                and sib_text not in usernames
                            ):
                                usernames.append(sib_text)
                                break  # 每个卡片只取第一个匹配
        except Exception as e:
            logger.debug(f"XML 解析用户名失败: {e}")

        return usernames

    def _find_parent(self, root, target):
        """在 XML 树中找目标节点的父节点。"""
        for parent in root.iter():
            for child in parent:
                if child is target:
                    return parent
        return None
