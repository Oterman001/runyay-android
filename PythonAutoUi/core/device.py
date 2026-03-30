"""
设备连接管理：封装 uiautomator2 设备对象，提供 App 启动、截图、崩溃监控。
"""

from __future__ import annotations

import time
from pathlib import Path
from typing import Optional

import uiautomator2 as u2
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_fixed

XHS_PACKAGE = "com.xingin.xhs"
XHS_ACTIVITY = "com.xingin.xhs/.splash.SplashActivity"


class Device:
    def __init__(self, serial: str = ""):
        self._serial = serial or None
        self.d: Optional[u2.Device] = None

    def connect(self) -> None:
        """连接设备并安装 ATX agent（首次需时约 30s）。"""
        logger.info(f"正在连接设备: {self._serial or '自动检测'}")
        self.d = u2.connect(self._serial)
        info = self.d.info
        logger.info(
            f"设备已连接: {info.get('productName', '?')} "
            f"Android {info.get('sdkInt', '?')} "
            f"屏幕 {info.get('displayWidth')}x{info.get('displayHeight')}"
        )
        self._register_watchers()

    def _register_watchers(self) -> None:
        """注册系统弹窗自动处理（崩溃、权限、更新提示）。"""
        d = self.d

        # App 崩溃弹窗
        d.watcher("crash_dialog").when(
            textContains="停止运行"
        ).press("back")
        d.watcher("crash_dialog2").when(
            textContains="已停止"
        ).press("back")

        # 系统权限弹窗（允许）
        d.watcher("allow_permission").when(
            textContains="允许"
        ).click(text="允许")

        # XHS 更新提示（跳过）
        d.watcher("xhs_update").when(
            textContains="发现新版本"
        ).click(text="以后再说")
        d.watcher("xhs_update2").when(
            textContains="暂不更新"
        ).click(text="暂不更新")

        d.watcher.start(interval=3.0)
        logger.debug("Watchers 已注册")

    def launch_xhs(self) -> None:
        """启动小红书，等待首页加载完成。"""
        logger.info("启动小红书...")
        self.d.app_start(XHS_PACKAGE, stop=False)
        self._wait_for_home(timeout=15)

    def restart_xhs(self) -> None:
        """强制重启小红书（用于恢复异常状态）。"""
        logger.warning("重启小红书...")
        self.d.app_stop(XHS_PACKAGE)
        time.sleep(2)
        self.d.app_start(XHS_PACKAGE)
        self._wait_for_home(timeout=20)

    @retry(stop=stop_after_attempt(3), wait=wait_fixed(3))
    def _wait_for_home(self, timeout: int = 15) -> None:
        """等待首页底部导航栏出现。"""
        if not self.d(text="首页").wait(timeout=timeout):
            raise RuntimeError("等待首页超时，未检测到底部导航栏")
        logger.debug("首页已就绪")

    def is_xhs_foreground(self) -> bool:
        """判断小红书是否在前台。"""
        return self.d.app_current().get("package", "") == XHS_PACKAGE

    def screenshot(self, save_path: Optional[str] = None) -> None:
        """截图，可选保存到文件（调试用）。"""
        img = self.d.screenshot()
        if save_path:
            Path(save_path).parent.mkdir(parents=True, exist_ok=True)
            img.save(save_path)
            logger.debug(f"截图已保存: {save_path}")
        return img

    def press_back(self) -> None:
        self.d.press("back")

    def press_home(self) -> None:
        self.d.press("home")
