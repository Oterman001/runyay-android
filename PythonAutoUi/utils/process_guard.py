"""
进程互斥守卫：确保同一时刻只有一个 UI 自动化脚本在运行。

用法（在任意脚本入口处调用）：
    from utils.process_guard import acquire_guard

    acquire_guard()   # 若有旧进程则先杀掉，再注册当前 PID
    # ... 业务逻辑 ...
    # 脚本退出时自动清理 PID 文件（atexit 注册）
"""

from __future__ import annotations

import atexit
import os
import signal
from pathlib import Path

import psutil
from loguru import logger

_PID_FILE = Path(__file__).parent.parent / "data" / "automation.pid"


def _read_pid() -> int | None:
    try:
        return int(_PID_FILE.read_text().strip())
    except Exception:
        return None


def _write_pid() -> None:
    _PID_FILE.parent.mkdir(parents=True, exist_ok=True)
    _PID_FILE.write_text(str(os.getpid()))


def _remove_pid() -> None:
    try:
        _PID_FILE.unlink(missing_ok=True)
    except Exception:
        pass


def _kill_process(pid: int) -> None:
    """终止指定进程及其子进程。"""
    try:
        proc = psutil.Process(pid)
        children = proc.children(recursive=True)
        for child in children:
            try:
                child.kill()
            except psutil.NoSuchProcess:
                pass
        proc.kill()
        proc.wait(timeout=5)
        logger.info(f"已终止旧自动化进程 (PID={pid})")
    except psutil.NoSuchProcess:
        logger.debug(f"旧进程 (PID={pid}) 已不存在，跳过")
    except psutil.TimeoutExpired:
        logger.warning(f"旧进程 (PID={pid}) 未能在 5s 内退出")
    except Exception as e:
        logger.warning(f"终止旧进程失败 (PID={pid}): {e}")


def acquire_guard() -> None:
    """
    检查是否有旧的自动化脚本正在运行：
    - 有且存活 → 先终止，再注册当前 PID
    - 无或已退出 → 直接注册当前 PID
    脚本退出时自动清理 PID 文件。
    """
    old_pid = _read_pid()
    current_pid = os.getpid()

    if old_pid and old_pid != current_pid:
        if psutil.pid_exists(old_pid):
            logger.warning(f"检测到旧自动化进程仍在运行 (PID={old_pid})，正在终止...")
            _kill_process(old_pid)
        else:
            logger.debug(f"旧 PID 文件残留 (PID={old_pid})，进程已退出，忽略")

    _write_pid()
    atexit.register(_remove_pid)
    logger.debug(f"进程守卫已激活 (PID={current_pid})")
