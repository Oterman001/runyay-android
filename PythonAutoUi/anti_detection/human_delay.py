"""
拟人化延迟：所有等待时间使用高斯分布，绝不使用固定值。
"""

from __future__ import annotations

import random
import time

from loguru import logger
from utils.config_loader import DelayConfig, DelaysConfig


def _gauss_sleep(mu: float, sigma: float, min_val: float = 0.1) -> float:
    """高斯随机睡眠，返回实际睡眠时长。"""
    duration = max(min_val, random.gauss(mu, sigma))
    time.sleep(duration)
    return duration


class HumanDelay:
    """封装所有场景的延迟，从 config 读取参数。"""

    def __init__(self, cfg: DelaysConfig):
        self._cfg = cfg

    def tap(self) -> None:
        """点击前短暂停顿（模拟人类反应时间）。"""
        _gauss_sleep(self._cfg.tap.mu, self._cfg.tap.sigma, min_val=0.2)

    def between_profiles(self) -> None:
        """浏览主页之间的间隔。"""
        _gauss_sleep(
            self._cfg.between_profiles.mu,
            self._cfg.between_profiles.sigma,
            min_val=1.0,
        )

    def post_follow(self) -> None:
        """关注动作后的间隔（最关键，影响被检测概率）。"""
        base = _gauss_sleep(
            self._cfg.post_follow.mu,
            self._cfg.post_follow.sigma,
            min_val=5.0,
        )
        # 15% 概率触发"深度阅读"长暂停
        if random.random() < self._cfg.long_break_probability:
            extra = random.uniform(
                self._cfg.long_break_extra_min,
                self._cfg.long_break_extra_max,
            )
            logger.debug(f"触发长暂停: +{extra:.1f}s")
            time.sleep(extra)

    def scroll_pause(self) -> None:
        """滚动翻页后的停顿（模拟阅读浏览）。"""
        _gauss_sleep(
            self._cfg.scroll_pause.mu,
            self._cfg.scroll_pause.sigma,
            min_val=0.5,
        )

    def session_break(self, seconds: int = 180) -> None:
        """单次会话超限后的长休息（加 ±20% 随机抖动）。"""
        jitter = random.uniform(0.8, 1.2)
        actual = int(seconds * jitter)
        logger.info(f"会话休息中，预计 {actual}s ...")
        time.sleep(actual)

    def typing_char(self) -> None:
        """逐字符输入时的按键间隔（50-150ms）。"""
        time.sleep(random.uniform(0.05, 0.15))
