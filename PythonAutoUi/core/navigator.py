"""
XHS 屏幕导航：提供命名屏幕切换，检测当前所在页面。
"""

from __future__ import annotations

import time

import uiautomator2 as u2
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_fixed


class Navigator:
    def __init__(self, d: u2.Device):
        self.d = d

    # ------------------------------------------------------------------ #
    # 当前屏幕检测
    # ------------------------------------------------------------------ #

    def current_screen(self) -> str:
        """
        返回当前所在屏幕名称：
            'home'      首页
            'search'    搜索结果页
            'profile'   用户主页
            'unknown'   未知
        """
        d = self.d
        if d(text="首页", className="android.widget.TextView").exists:
            # 底部导航栏有"首页"说明在主页面族
            if d(text="搜索", className="android.widget.EditText").exists:
                return "search_input"
            # 检查是否有"粉丝"数字区域（主页特征）
            if d(textContains="粉丝").exists and not d(text="发现").exists:
                return "profile"
            if d(textContains="综合").exists or d(text="用户").exists:
                return "search_results"
            return "home"
        return "unknown"

    # ------------------------------------------------------------------ #
    # 导航动作
    # ------------------------------------------------------------------ #

    def go_home(self) -> None:
        """点击底部"首页"Tab。"""
        self.d(text="首页").click()
        time.sleep(1.0)
        logger.debug("已回到首页")

    @retry(stop=stop_after_attempt(3), wait=wait_fixed(1))
    def go_to_search(self) -> None:
        """从首页进入搜索输入框。"""
        d = self.d
        # 搜索图标可能是 content-desc 或 text
        if d(contentDescription="搜索").exists:
            d(contentDescription="搜索").click()
        elif d(text="搜索").exists:
            d(text="搜索").click()
        else:
            # 右上角放大镜 icon（fallback）
            d.xpath('//*[@content-desc="搜索" or @text="搜索"]').click()

        # 等待搜索输入框获得焦点
        if not d(focused=True, className="android.widget.EditText").wait(timeout=5):
            raise RuntimeError("搜索输入框未出现")
        logger.debug("搜索输入框已就绪")

    def go_back(self) -> None:
        """返回上一页。"""
        self.d.press("back")
        time.sleep(0.8)

    def go_back_to_search_results(self) -> None:
        """从用户主页返回搜索结果列表。"""
        self.go_back()
        # 等待搜索结果标签出现
        self.d(text="用户").wait(timeout=5)
        logger.debug("已返回搜索结果")

    # ------------------------------------------------------------------ #
    # 推荐列表路径导航（v2）
    # ------------------------------------------------------------------ #

    def go_to_my_tab(self) -> None:
        """
        点击底部'我' Tab，进入我的主页。
        实机验证：XHS 底部 Tab 全部使用 text 属性，contentDescription 为空。
        """
        d = self.d
        # text="我" 是底部最右侧的 Tab
        found = d(text="我").wait(timeout=8)
        if not found:
            raise RuntimeError("底部'我' Tab 未找到")
        d(text="我").click()
        time.sleep(2.0)
        # 等待我的主页加载：「编辑资料」Button 唯一出现在我的主页，不在关注列表/首页
        # 不使用 text='粉丝'/'关注'，这两个 Tab 在关注列表顶部也存在，会立即误判
        found = (
            d(text="编辑资料", className="android.widget.Button").wait(timeout=12)
            or d(textContains="编辑").wait(timeout=3)  # 兼容部分版本文字差异
        )
        if not found:
            # 有时加载慢，press_back 清理后再试一次
            logger.debug("'编辑资料'未出现，清理后重试...")
            d.press("back")
            time.sleep(1.5)
            d(text="我").wait(timeout=5)
            d(text="我").click()
            time.sleep(3.0)
            found = (
                d(text="编辑资料", className="android.widget.Button").wait(timeout=12)
                or d(textContains="编辑").wait(timeout=3)
            )
        if not found:
            raise RuntimeError("我的主页未加载（未找到'编辑资料'按钮）")
        logger.debug("已进入'我的'主页")

    def click_following_count(self) -> None:
        """
        在我的主页，点击'关注'数字 Button 进入关注列表。
        实机验证：统计区是一行 3 个空文本 Button，从左到右依次是：
          Button[0] = 关注数（x≈49-151），Button[1] = 粉丝数，Button[2] = 获赞收藏
        直接点击最左侧的 Button 即可进入关注列表。
        """
        d = self.d
        import xml.etree.ElementTree as ET

        # 获取统计行中最左侧的 Button（即关注数按钮）
        clicked = False
        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            stat_buttons = []
            for node in root.iter():
                cls = node.get("class", "")
                b = node.get("bounds", "")
                t = node.get("text", "").strip()
                if "Button" in cls and not t and b:
                    try:
                        parts = b.replace("][", ",").strip("[]").split(",")
                        l, top, r, bot = int(parts[0]), int(parts[1]), int(parts[2]), int(parts[3])
                        h = bot - top
                        # 统计 Button 高度约 100px，宽约 100-200px，排除全屏 Button
                        if 60 < h < 160 and (r - l) < 400:
                            stat_buttons.append((l, top, r, bot))
                    except Exception:
                        pass
            if stat_buttons:
                # 只取 y_top 在 600-1100 范围内的（统计区，排除其他行按钮）
                stat_buttons = [(l, top, r, bot) for l, top, r, bot in stat_buttons if 600 < top < 1100]
                # 按 x 坐标排序，取最左侧（关注数按钮）
                stat_buttons.sort(key=lambda x: x[0])
                l, top, r, bot = stat_buttons[0]
                cx = (l + r) // 2
                cy = (top + bot) // 2
                logger.debug(f"点击关注数按钮: ({cx},{cy})")
                d.click(cx, cy)
                clicked = True
        except Exception as e:
            logger.debug(f"统计按钮定位失败: {e}")

        if not clicked:
            # fallback：直接点击 text='关注' 标签区域（在统计行左侧）
            if d(text="关注").exists:
                d(text="关注").click()
            else:
                d.click(100, 880)  # 固定坐标兜底

        time.sleep(1.8)
        # 等待关注列表页加载：'推荐' Tab 出现（text 属性）
        if not d(text="推荐").wait(timeout=8):
            raise RuntimeError("底部'我' Tab 未找到")
        logger.debug("已进入关注列表页")

    @retry(stop=stop_after_attempt(3), wait=wait_fixed(1))
    def switch_to_recommend_tab(self) -> None:
        """
        在关注列表页，点击顶部'推荐' Tab。
        实机验证：Tab 使用 text 属性，不是 description。
        """
        d = self.d
        if not d(text="推荐").wait(timeout=5):
            raise RuntimeError("未找到'推荐' Tab")
        d(text="推荐").click()
        time.sleep(2.0)
        logger.debug("已切换到推荐列表")

    def go_back_to_recommend_list(self, depth: int = 1) -> None:
        """
        从用户主页（可能有多层深入）返回推荐列表。
        depth: 需要按返回键的次数。
        """
        for _ in range(depth):
            self.d.press("back")
            time.sleep(0.8)
        # 确认推荐 Tab 可见（在关注列表页）
        self.d(description="推荐").wait(timeout=5)
        logger.debug(f"已返回推荐列表（back x{depth}）")

    # ------------------------------------------------------------------ #
    # 读取「我的」统计数据
    # ------------------------------------------------------------------ #

    def read_my_stats(self) -> dict:
        """
        导航到「我的」Tab，读取并返回当前账号的关注数与粉丝数。
        返回格式：{"following": int, "followers": int}
        使用与 profile_parser 相同的兄弟节点解析策略。
        """
        import re
        import xml.etree.ElementTree as ET
        from utils.cn_number import parse_cn_number

        d = self.d
        result = {"following": 0, "followers": 0}

        # 进入我的主页
        self.go_to_my_tab()
        time.sleep(1.0)

        # dump 页面 XML，用兄弟节点法提取数值
        xml_str = d.dump_hierarchy()
        root = ET.fromstring(xml_str)

        label_map = {"关注": "following", "粉丝": "followers"}
        for label, key in label_map.items():
            for parent in root.iter():
                children = list(parent)
                for i, child in enumerate(children):
                    if (
                        child.get("text", "").strip() == label
                        and "TextView" in child.get("class", "")
                    ):
                        # 向前找第一个含数字的兄弟节点
                        for j in range(i - 1, -1, -1):
                            num_text = children[j].get("text", "").strip()
                            if num_text and re.search(r"\d", num_text):
                                result[key] = parse_cn_number(num_text)
                                break
                        break
                if result[key]:
                    break

        logger.info(
            f"我的账号统计 — 关注: {result['following']}  粉丝: {result['followers']}"
        )
        return result
