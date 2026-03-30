"""
日志模块：基于 loguru，输出到终端（rich 着色）和日志文件。
"""

import sys
from pathlib import Path
from loguru import logger

_initialized = False


def setup_logger(log_dir: str = "data/logs", level: str = "DEBUG") -> None:
    global _initialized
    if _initialized:
        return

    Path(log_dir).mkdir(parents=True, exist_ok=True)

    # 移除默认 handler
    logger.remove()

    # 终端输出（彩色，INFO+）
    logger.add(
        sys.stderr,
        level="INFO",
        colorize=True,
        format=(
            "<green>{time:HH:mm:ss}</green> | "
            "<level>{level: <8}</level> | "
            "<cyan>{name}</cyan>:<cyan>{line}</cyan> — <level>{message}</level>"
        ),
    )

    # 文件输出（DEBUG+，按天滚动，保留 14 天）
    logger.add(
        f"{log_dir}/run_{{time:YYYYMMDD}}.log",
        level=level,
        rotation="00:00",
        retention="14 days",
        encoding="utf-8",
        format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {name}:{line} — {message}",
    )

    _initialized = True


# 默认初始化
setup_logger()
