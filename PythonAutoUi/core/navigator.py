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
        """点击底部'我' Tab，进入我的主页。"""
        d = self.d
        # 底部'我' Tab 的 content-desc 就是"我"
        if not d(description="我").wait(timeout=8):
            raise RuntimeError("底部'我' Tab 未找到")
        d(description="我").click()
        time.sleep(2.0)
        # 等待主页统计区域（关注/粉丝按钮）出现，冷启动时可能较慢
        found = (
            d(descriptionContains="关注", className="android.widget.Button").wait(timeout=15)
            or d(descriptionContains="粉丝", className="android.widget.Button").wait(timeout=5)
        )
        if not found:
            raise RuntimeError("我的主页统计区域未加载")
        logger.debug("已进入'我的'主页")

    def click_following_count(self) -> None:
        """
        在我的主页，点击'关注'数字 Button 进入关注列表。
        Button 的 content-desc 格式为 "NNN关注"（如 "456关注"）。
        """
        d = self.d
        # 找 content-desc 以'关注'结尾的 Button（排除'已关注'按钮）
        btn = d.xpath(
            '//android.widget.Button[contains(@content-desc,"关注") '
            'and not(@text="关注") and not(@text="已关注")]'
        )
        if btn.exists:
            btn.click()
        else:
            # fallback：坐标点击（主页统计行左侧第一个按钮）
            screen_w = d.info.get("displayWidth", 1116)
            d.click(screen_w // 10, 870)  # 近似坐标
        time.sleep(1.8)
        # 等待关注列表页的推荐 Tab 出现
        if not d(description="推荐").wait(timeout=8):
            raise RuntimeError("关注列表页未加载（未找到'推荐' Tab）")
        logger.debug("已进入关注列表页")

    @retry(stop=stop_after_attempt(3), wait=wait_fixed(1))
    def switch_to_recommend_tab(self) -> None:
        """在关注列表页，点击顶部'推荐' Tab。"""
        d = self.d
        if not d(description="推荐").wait(timeout=5):
            raise RuntimeError("未找到'推荐' Tab")
        d(description="推荐").click()
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
