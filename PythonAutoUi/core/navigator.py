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
