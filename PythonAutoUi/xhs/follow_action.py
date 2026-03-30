"""
关注动作执行器：点击"关注"按钮并验证结果。
"""

from __future__ import annotations

import time

import uiautomator2 as u2
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_fixed

from anti_detection.gesture import human_tap
from anti_detection.human_delay import HumanDelay
from persistence.models import BloggerProfile

# 关注成功后按钮应变成的文字
_SUCCESS_STATES = {"已关注", "互相关注"}
# 可点击的关注按钮文字
_FOLLOWABLE_TEXTS = ["关注", "+ 关注", "+关注"]


class FollowAction:
    def __init__(self, d: u2.Device, delay: HumanDelay):
        self._d = d
        self._delay = delay

    def follow(self, profile: BloggerProfile, dry_run: bool = False) -> bool:
        """
        对当前主页执行关注。

        Args:
            profile:  博主信息（用于日志）
            dry_run:  True 时只打日志，不实际点击

        Returns:
            True 表示关注成功（或 dry_run 模拟成功）
        """
        d = self._d

        # 确保"关注"按钮可见（可能需滚动）
        btn = self._find_follow_button()
        if btn is None:
            logger.warning(f"未找到'关注'按钮: {profile.username}")
            return False

        if dry_run:
            logger.info(f"[DRY RUN] 模拟关注: {profile.username} (评分={profile.follow_score})")
            return True

        # 点击前短暂停顿
        self._delay.tap()

        logger.info(f"关注: {profile.username} (粉丝={profile.followers} 评分={profile.follow_score})")
        human_tap(d, btn)

        # 等待 UI 更新（profile 页面按钮动画需要更长时间）
        time.sleep(2.5)

        # 验证关注是否成功，最多再等 2s 轮询
        confirmed = self._verify_follow_success()
        if not confirmed:
            time.sleep(2.0)
            confirmed = self._verify_follow_success()

        if confirmed:
            logger.success(f"关注成功: {profile.username}")
        else:
            logger.warning(f"关注验证未通过，可能已关注或操作失败: {profile.username}")

        # 检查是否触发了平台限制警告
        self._check_rate_limit_warning()

        return confirmed

    # ------------------------------------------------------------------ #
    # 内部方法
    # ------------------------------------------------------------------ #

    def _find_follow_button(self):
        """找到可点击的"关注"按钮，必要时向上滚动使其可见。"""
        d = self._d
        for text in _FOLLOWABLE_TEXTS:
            btn = d(text=text)
            if btn.exists:
                return btn

        # 尝试向上滚动一次，再找
        from anti_detection.gesture import human_swipe_down
        human_swipe_down(d, distance=400)
        time.sleep(0.8)

        for text in _FOLLOWABLE_TEXTS:
            btn = d(text=text)
            if btn.exists:
                return btn

        return None

    def _verify_follow_success(self) -> bool:
        """验证关注按钮是否变为成功状态。"""
        d = self._d
        for state_text in _SUCCESS_STATES:
            if d(text=state_text).exists:
                return True
        return False

    def _check_rate_limit_warning(self) -> None:
        """检测平台限制弹窗（如"关注了太多人"）并记录告警。"""
        d = self._d
        warning_texts = [
            "关注了太多",
            "操作过于频繁",
            "请稍后再试",
            "账号异常",
        ]
        for text in warning_texts:
            if d(textContains=text).exists:
                logger.error(f"检测到平台限制警告: '{text}'，建议停止当前会话！")
                # 关闭弹窗
                if d(text="确定").exists:
                    d(text="确定").click()
                elif d(text="我知道了").exists:
                    d(text="我知道了").click()
                raise RateLimitWarning(f"平台限制: {text}")


class RateLimitWarning(Exception):
    """平台关注频率限制警告，触发时应立即停止会话。"""
    pass
