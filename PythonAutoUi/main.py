"""
小红书跑步博主自动关注 — 主入口

用法：
  python main.py                        # 自动整合模式（默认），随机顺序运行两种路径
  python main.py --mode auto            # 同上，显式指定
  python main.py --mode recommend       # 仅推荐列表模式
  python main.py --mode my_following    # 仅我的关注列表探索模式
  python main.py --dry-run              # 模拟运行，不实际关注
  python main.py --limit 5              # 覆盖每日限额
  python main.py --serial xxxxx         # 指定设备序列号
"""

from __future__ import annotations

import argparse
import random
import sys
import time
from pathlib import Path

# 将项目根目录加入 Python 路径
sys.path.insert(0, str(Path(__file__).parent))

from loguru import logger
from rich.console import Console
from rich.table import Table

import utils.logger  # 初始化日志
from anti_detection.human_delay import HumanDelay
from core.device import Device
from core.navigator import Navigator
from persistence.database import Database
from utils.config_loader import load_config
from xhs.follow_action import FollowAction, RateLimitWarning
from xhs.profile_parser import ProfileParser
from xhs.recommender import Recommender

console = Console()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="小红书跑步博主自动关注脚本")
    parser.add_argument("--dry-run", action="store_true", help="模拟运行，不实际关注")
    parser.add_argument("--limit", type=int, default=0, help="覆盖每日关注限额")
    parser.add_argument("--serial", type=str, default="", help="设备序列号")
    parser.add_argument(
        "--mode",
        choices=["auto", "recommend", "my_following"],
        default="",
        help="运行模式：auto（随机整合，默认）/ recommend（推荐列表）/ my_following（我的关注列表探索）",
    )
    return parser.parse_args()


# ──────────────────────────────────────────────────────────────────────────────
# 推荐列表模式（v2，主要使用）
# ──────────────────────────────────────────────────────────────────────────────

def run_recommend_session(config, db: Database, device: Device) -> int:
    """
    执行一次基于推荐列表路径的关注会话。
    遇到 ATX 断线（RemoteDisconnected / ConnectionError）时最多重连 2 次后继续。
    返回本次会话的 session_id（0 表示异常未创建）。
    """
    import http.client

    max_reconnects = 2
    reconnect_count = 0

    while reconnect_count <= max_reconnects:
        nav = Navigator(device.d)
        delay = HumanDelay(config.delays)
        parser = ProfileParser(device.d)
        follower = FollowAction(device.d, delay)

        session = db.create_session(["[推荐列表路径]"])

        try:
            recommender = Recommender(
                d=device.d,
                navigator=nav,
                delay=delay,
                parser=parser,
                follower=follower,
                db=db,
                config=config,
                session_id=session.id,
                device=device,
            )
            total_followed = recommender.run()
            session.follows_made = total_followed
            session.stop_reason = "complete"
            db.close_session(session)
            _print_session_summary(session, db)
            return session.id  # 正常完成

        except RateLimitWarning as e:
            logger.error(f"平台限制，立即停止: {e}")
            session.stop_reason = "rate_limit"
            db.close_session(session)
            _print_session_summary(session, db)
            return session.id

        except KeyboardInterrupt:
            session.stop_reason = "interrupted"
            logger.warning("用户中断")
            db.close_session(session)
            _print_session_summary(session, db)
            return session.id

        except (http.client.RemoteDisconnected, ConnectionError, OSError) as e:
            reconnect_count += 1
            session.stop_reason = f"disconnected:{e}"
            session.follows_made = recommender._follows_this_session if 'recommender' in dir() else 0
            db.close_session(session)
            if reconnect_count > max_reconnects:
                logger.error(f"ATX 断线超过重连次数上限，放弃: {e}")
                _print_session_summary(session, db)
                return session.id
            logger.warning(f"ATX 断线，第 {reconnect_count} 次重连中（等待 10s）: {e}")
            time.sleep(10)
            try:
                device.connect()
                device.launch_xhs()
                logger.info("重连成功，继续会话")
            except Exception as re_err:
                logger.error(f"重连失败: {re_err}")
                _print_session_summary(session, db)
                return session.id

        except Exception as e:
            session.stop_reason = f"error: {e}"
            logger.exception(f"推荐会话异常: {e}")
            db.close_session(session)
            _print_session_summary(session, db)
            return session.id

    return 0


# ──────────────────────────────────────────────────────────────────────────────
# 我的关注列表探索模式（v3）
# ──────────────────────────────────────────────────────────────────────────────

def run_my_following_session(config, db: Database, device: Device) -> int:
    """
    从我的关注列表出发，进入各已关注博主的关注列表探索新跑友。
    返回本次会话的 session_id（0 表示异常未创建）。
    """
    import http.client

    max_reconnects = 2
    reconnect_count = 0

    while reconnect_count <= max_reconnects:
        nav = Navigator(device.d)
        delay = HumanDelay(config.delays)
        parser = ProfileParser(device.d)
        follower = FollowAction(device.d, delay)

        session = db.create_session(["[我的关注列表探索]"])

        try:
            recommender = Recommender(
                d=device.d,
                navigator=nav,
                delay=delay,
                parser=parser,
                follower=follower,
                db=db,
                config=config,
                session_id=session.id,
                device=device,
            )
            total_followed = recommender.run()
            session.follows_made = total_followed
            session.stop_reason = "complete"
            db.close_session(session)
            _print_session_summary(session, db)
            return session.id

        except RateLimitWarning as e:
            logger.error(f"平台限制，立即停止: {e}")
            session.stop_reason = "rate_limit"
            db.close_session(session)
            _print_session_summary(session, db)
            return session.id

        except KeyboardInterrupt:
            session.stop_reason = "interrupted"
            logger.warning("用户中断")
            db.close_session(session)
            _print_session_summary(session, db)
            return session.id

        except (http.client.RemoteDisconnected, ConnectionError, OSError) as e:
            reconnect_count += 1
            session.stop_reason = f"disconnected:{e}"
            session.follows_made = recommender._follows_this_session if 'recommender' in dir() else 0
            db.close_session(session)
            if reconnect_count > max_reconnects:
                logger.error(f"ATX 断线超过重连次数上限，放弃: {e}")
                _print_session_summary(session, db)
                return session.id
            logger.warning(f"ATX 断线，第 {reconnect_count} 次重连中（等待 10s）: {e}")
            time.sleep(10)
            try:
                device.connect()
                device.launch_xhs()
                logger.info("重连成功，继续会话")
            except Exception as re_err:
                logger.error(f"重连失败: {re_err}")
                _print_session_summary(session, db)
                return session.id

        except Exception as e:
            session.stop_reason = f"error: {e}"
            logger.exception(f"我的关注列表探索会话异常: {e}")
            db.close_session(session)
            _print_session_summary(session, db)
            return session.id

    return 0


# ──────────────────────────────────────────────────────────────────────────────
# 自动整合模式（默认）：随机顺序交替两种路径，模拟真实用户行为
# ──────────────────────────────────────────────────────────────────────────────

def run_auto_session(config, db: Database, device: Device) -> int:
    """
    自动整合模式：将推荐列表和我的关注列表两种路径随机排序后依次执行，
    模拟真实用户自然切换浏览行为。

    - 每段路径开始前检查每日限额，达到上限则提前结束
    - 两段路径之间加入随机间隔（15-45s），模拟用户自然停顿
    - 返回最后一个完成会话的 session_id（0 表示全部异常）
    """
    from strategy.daily_limit import can_follow

    modes = ["recommend", "my_following"]
    random.shuffle(modes)

    logger.info(f"自动整合模式，本次路径顺序：{' → '.join(modes)}")

    last_session_id = 0

    for i, mode in enumerate(modes):
        if not can_follow(db, config):
            logger.info(f"已达每日关注上限，跳过后续路径 [{mode}]")
            break

        logger.info(f"[{i + 1}/{len(modes)}] 切换到路径：{mode}")
        config.mode = mode

        if mode == "recommend":
            session_id = run_recommend_session(config, db, device)
        else:
            session_id = run_my_following_session(config, db, device)

        if session_id:
            last_session_id = session_id

        # 路径切换间随机停顿（最后一段不停顿）
        if i < len(modes) - 1:
            pause = random.uniform(15, 45)
            logger.info(f"路径切换，休息 {pause:.0f}s 后继续...")
            time.sleep(pause)

    return last_session_id


# ──────────────────────────────────────────────────────────────────────────────
# 公共工具
# ──────────────────────────────────────────────────────────────────────────────

def _print_session_summary(session, db: Database) -> None:
    """用 rich 打印会话总结。"""
    table = Table(title="会话总结", show_header=True, header_style="bold cyan")
    table.add_column("指标", style="dim")
    table.add_column("值", justify="right")
    table.add_row("扫描候选", str(session.candidates_seen))
    table.add_row("符合条件", str(session.candidates_qualified))
    table.add_row("成功关注", str(session.follows_made))
    table.add_row("今日累计", str(db.get_today_follow_count()))
    table.add_row("停止原因", session.stop_reason)
    console.print(table)


def _print_my_stats_table(stats: dict) -> None:
    """打印我的账号统计表格。"""
    table = Table(title="我的账号当前统计", show_header=True, header_style="bold magenta")
    table.add_column("指标", style="dim")
    table.add_column("数值", justify="right", style="bold")
    table.add_row("我的关注数", str(stats.get("following", 0)))
    table.add_row("我的粉丝数", str(stats.get("followers", 0)))
    console.print(table)


def _post_session_stats(device: Device) -> dict:
    """
    会话结束后读取账号统计，两种方式结合：

    方式1（优先）：导航到「我的」Tab → 下拉刷新 2-3 次 → 读取
      - 快速、自然，无需重启 App
      - 若成功获取非零数据，直接返回

    方式2（兜底）：数据为 0 时，杀进程 → 随机等待 → 冷启动 → 重新读取
      - 确保数据准确，等待时间随机化避免固定特征

    返回 {"following": int, "followers": int}，失败时返回空字典。
    """
    from core.device import XHS_PACKAGE

    d = device.d
    try:
        # ── 方式1：下拉刷新读取（快速、自然）────────────────────────────
        logger.info("会话结束，下拉刷新读取最新统计...")
        nav = Navigator(d)
        stats = nav.read_my_stats(pull_refresh=True)

        if stats.get("following", 0) > 0 or stats.get("followers", 0) > 0:
            _print_my_stats_table(stats)
            return stats

        # ── 方式2：冷启动重试（数据仍为 0 时兜底）───────────────────────
        logger.warning("下拉刷新后统计数据为 0，改用冷启动重新读取...")
        d.app_stop(XHS_PACKAGE)
        time.sleep(random.uniform(1.5, 3.5))

        device.launch_xhs()
        time.sleep(random.uniform(2.0, 4.0))

        nav2 = Navigator(d)
        stats = nav2.read_my_stats()
        _print_my_stats_table(stats)
        return stats

    except Exception as e:
        logger.warning(f"收尾统计读取失败（不影响主流程）: {e}")
        return {}


def main() -> None:
    args = parse_args()
    config = load_config()

    # 命令行参数覆盖配置
    if args.dry_run:
        config.dry_run = True
    if args.limit > 0:
        config.daily_follow_limit = args.limit
    if args.serial:
        config.device.serial = args.serial
    if args.mode:
        config.mode = args.mode

    run_mode = "DRY RUN" if config.dry_run else "正式运行"
    console.print(f"\n[bold green]小红书跑步博主自动关注[/bold green] — {run_mode}")
    console.print(
        f"模式: [bold]{config.mode}[/bold] | "
        f"每日上限: {config.daily_follow_limit} | "
        f"评分阈值: {config.min_score_threshold} | "
        f"最大深度: {config.max_depth}"
    )

    db = Database()
    device = Device(serial=config.device.serial)

    try:
        device.connect()
        device.launch_xhs()

        if config.mode == "my_following":
            session_id = run_my_following_session(config, db, device)
        elif config.mode == "recommend":
            session_id = run_recommend_session(config, db, device)
        else:  # auto（默认）
            session_id = run_auto_session(config, db, device)

        # 会话正常结束后：关闭 App → 冷启动 → 读取我的关注/粉丝统计并存库
        stats = _post_session_stats(device)
        if session_id and stats:
            db.update_session_account_stats(
                session_id,
                stats.get("following", 0),
                stats.get("followers", 0),
            )

    except Exception as e:
        logger.exception(f"主流程异常: {e}")
    finally:
        db.close()
        device.press_home()
        logger.info("脚本退出")


if __name__ == "__main__":
    main()
