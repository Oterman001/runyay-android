"""
每日限额检查：基于数据库中今日关注记录数。
"""

from __future__ import annotations

from datetime import datetime

from loguru import logger

from persistence.database import Database
from utils.config_loader import AppConfig


def can_follow(db: Database, config: AppConfig) -> bool:
    """是否还可以关注（未超每日限额 且 在活跃时段内）。"""
    if not _is_active_hour(config):
        logger.info(
            f"当前不在活跃时段 {config.active_hours[0]}:00-{config.active_hours[1]}:00，跳过"
        )
        return False

    count = db.get_today_follow_count()
    if count >= config.daily_follow_limit:
        logger.warning(f"今日已关注 {count} 人，达到每日上限 {config.daily_follow_limit}")
        return False

    logger.debug(f"今日已关注 {count}/{config.daily_follow_limit}")
    return True


def _is_active_hour(config: AppConfig) -> bool:
    """当前小时是否在 active_hours 范围内。"""
    now_hour = datetime.now().hour
    start, end = config.active_hours
    return start <= now_hour < end
