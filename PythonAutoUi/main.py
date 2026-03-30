"""
小红书跑步博主自动关注 — 主入口

用法：
  python main.py                        # 推荐列表模式，读 config.yaml
  python main.py --mode recommend       # 推荐列表模式（默认）
  python main.py --mode search          # 关键词搜索模式
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
from strategy.daily_limit import can_follow
from strategy.scoring import compute_follow_score
from utils.config_loader import load_config
from xhs.follow_action import FollowAction, RateLimitWarning
from xhs.profile_parser import ProfileParser
from xhs.recommender import Recommender
from xhs.searcher import Searcher

console = Console()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="小红书跑步博主自动关注脚本")
    parser.add_argument("--dry-run", action="store_true", help="模拟运行，不实际关注")
    parser.add_argument("--limit", type=int, default=0, help="覆盖每日关注限额")
    parser.add_argument("--serial", type=str, default="", help="设备序列号")
    parser.add_argument(
        "--mode",
        choices=["search", "recommend"],
        default="",
        help="运行模式：recommend（推荐列表，默认）或 search（关键词搜索）",
    )
    return parser.parse_args()


# ──────────────────────────────────────────────────────────────────────────────
# 推荐列表模式（v2，主要使用）
# ──────────────────────────────────────────────────────────────────────────────

def run_recommend_session(config, db: Database, device: Device) -> None:
    """执行一次基于推荐列表路径的关注会话。"""
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
        )
        total_followed = recommender.run()
        session.follows_made = total_followed
        session.stop_reason = "complete"

    except RateLimitWarning as e:
        logger.error(f"平台限制，立即停止: {e}")
        session.stop_reason = "rate_limit"
    except KeyboardInterrupt:
        session.stop_reason = "interrupted"
        logger.warning("用户中断")
    except Exception as e:
        session.stop_reason = f"error: {e}"
        logger.exception(f"推荐会话异常: {e}")
    finally:
        db.close_session(session)
        _print_session_summary(session, db)


# ──────────────────────────────────────────────────────────────────────────────
# 搜索关键词模式（v1，保留）
# ──────────────────────────────────────────────────────────────────────────────

def run_search_session(config, db: Database, device: Device) -> None:
    """执行一次基于关键词搜索路径的关注会话。"""
    nav = Navigator(device.d)
    delay = HumanDelay(config.delays)
    searcher = Searcher(device.d, nav, delay)
    parser = ProfileParser(device.d)
    follower = FollowAction(device.d, delay)

    keywords = list(config.keywords)
    random.shuffle(keywords)
    session = db.create_session(keywords)
    followed_this_session = 0

    try:
        for keyword in keywords:
            if not can_follow(db, config):
                session.stop_reason = "daily_limit"
                break

            try:
                candidates = searcher.search_users(keyword, config.pages_per_keyword)
            except Exception as e:
                logger.error(f"搜索关键词 '{keyword}' 失败: {e}")
                device.restart_xhs()
                continue

            session.candidates_seen += len(candidates)

            for username in candidates:
                if not can_follow(db, config):
                    session.stop_reason = "daily_limit"
                    return

                if followed_this_session >= config.max_follows_per_session:
                    logger.info(f"单次会话上限，休息 {config.session_break_seconds}s")
                    delay.session_break(config.session_break_seconds)
                    followed_this_session = 0

                if db.is_already_processed(username):
                    continue
                if random.random() < config.random_skip_ratio:
                    continue

                profile = None
                try:
                    btn = device.d(text=username)
                    if not btn.exists:
                        continue
                    btn.click()
                    time.sleep(1.5)
                    profile = parser.parse(username)
                except Exception as e:
                    logger.warning(f"进入主页失败 {username}: {e}")
                    device.press_back()
                    delay.between_profiles()
                    continue

                if profile is None:
                    device.press_back()
                    delay.between_profiles()
                    continue

                score = compute_follow_score(profile)
                profile.follow_score = score
                db.save_blogger(profile)

                if score < 0 or score < config.min_score_threshold:
                    device.press_back()
                    delay.between_profiles()
                    continue

                session.candidates_qualified += 1

                try:
                    success = follower.follow(profile, dry_run=config.dry_run)
                    if success:
                        db.record_follow(username, session.id, confirmed=True)
                        followed_this_session += 1
                        session.follows_made += 1
                        delay.post_follow()
                except RateLimitWarning as e:
                    logger.error(f"平台限制，停止: {e}")
                    session.stop_reason = "rate_limit"
                    device.press_back()
                    return
                except Exception as e:
                    logger.warning(f"关注失败 {username}: {e}")

                nav.go_back_to_search_results()
                delay.between_profiles()

    except KeyboardInterrupt:
        session.stop_reason = "interrupted"
    except Exception as e:
        session.stop_reason = f"error: {e}"
        logger.exception(f"搜索会话异常: {e}")
    finally:
        if not session.stop_reason:
            session.stop_reason = "complete"
        db.close_session(session)
        _print_session_summary(session, db)


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

        if config.mode == "recommend":
            run_recommend_session(config, db, device)
        else:
            run_search_session(config, db, device)

    except Exception as e:
        logger.exception(f"主流程异常: {e}")
    finally:
        db.close()
        device.press_home()
        logger.info("脚本退出")


if __name__ == "__main__":
    main()
