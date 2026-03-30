"""
小红书跑步博主自动关注 — 主入口

用法：
  python main.py                  # 正常运行（从 config.yaml 读取 dry_run 设置）
  python main.py --dry-run        # 强制 dry run 模式
  python main.py --limit 5        # 覆盖每日限额
  python main.py --serial xxxxx   # 指定设备序列号
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
from xhs.searcher import Searcher

console = Console()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="小红书跑步博主自动关注脚本")
    parser.add_argument("--dry-run", action="store_true", help="模拟运行，不实际关注")
    parser.add_argument("--limit", type=int, default=0, help="覆盖每日关注限额")
    parser.add_argument("--serial", type=str, default="", help="设备序列号")
    return parser.parse_args()


def run_session(config, db: Database, device: Device) -> None:
    """执行一次完整的搜索-分析-关注会话。"""
    nav = Navigator(device.d)
    delay = HumanDelay(config.delays)
    searcher = Searcher(device.d, nav, delay)
    parser = ProfileParser(device.d)
    follower = FollowAction(device.d, delay)

    # 关键词随机打乱顺序
    keywords = list(config.keywords)
    random.shuffle(keywords)

    session = db.create_session(keywords)
    followed_this_session = 0

    try:
        for keyword in keywords:
            if not can_follow(db, config):
                session.stop_reason = "daily_limit"
                break

            # --- 搜索候选用户 ---
            try:
                candidates = searcher.search_users(keyword, config.pages_per_keyword)
            except Exception as e:
                logger.error(f"搜索关键词 '{keyword}' 失败: {e}")
                device.restart_xhs()
                continue

            session.candidates_seen += len(candidates)

            # --- 处理每位候选 ---
            for username in candidates:
                # 每日限额检查
                if not can_follow(db, config):
                    session.stop_reason = "daily_limit"
                    logger.warning("每日关注上限已达，停止当前会话")
                    return

                # 单次会话限额检查
                if followed_this_session >= config.max_follows_per_session:
                    logger.info(
                        f"单次会话关注 {followed_this_session} 人，强制休息 "
                        f"{config.session_break_seconds}s"
                    )
                    delay.session_break(config.session_break_seconds)
                    followed_this_session = 0

                # 去重检查
                if db.is_already_processed(username):
                    logger.debug(f"已处理过: {username}，跳过")
                    continue

                # 随机跳过（模拟人类非系统性行为）
                if random.random() < config.random_skip_ratio:
                    logger.debug(f"随机跳过: {username}")
                    continue

                # --- 进入主页解析 ---
                profile = None
                try:
                    # 在搜索结果页点击该用户（由 searcher 确保当前在搜索结果页）
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

                # 计算评分
                score = compute_follow_score(profile)
                profile.follow_score = score
                db.save_blogger(profile)

                if score < 0:
                    logger.debug(f"硬过滤: {username}")
                    device.press_back()
                    delay.between_profiles()
                    continue

                if score < config.min_score_threshold:
                    logger.debug(f"评分不足 ({score}<{config.min_score_threshold}): {username}")
                    device.press_back()
                    delay.between_profiles()
                    continue

                session.candidates_qualified += 1

                # --- 执行关注 ---
                try:
                    success = follower.follow(profile, dry_run=config.dry_run)
                    if success:
                        db.record_follow(username, session.id, confirmed=True)
                        followed_this_session += 1
                        session.follows_made += 1
                        delay.post_follow()
                except RateLimitWarning as e:
                    logger.error(f"平台限制，立即停止: {e}")
                    session.stop_reason = "rate_limit"
                    device.press_back()
                    return
                except Exception as e:
                    logger.warning(f"关注失败 {username}: {e}")

                # 返回搜索结果页
                nav.go_back_to_search_results()
                delay.between_profiles()

    except KeyboardInterrupt:
        session.stop_reason = "interrupted"
        logger.warning("用户中断")
    except Exception as e:
        session.stop_reason = f"error: {e}"
        logger.exception(f"会话异常: {e}")
    finally:
        if not session.stop_reason:
            session.stop_reason = "complete"
        db.close_session(session)
        _print_session_summary(session, db)


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

    mode = "DRY RUN" if config.dry_run else "正式运行"
    console.print(f"\n[bold green]小红书跑步博主自动关注[/bold green] — {mode}")
    console.print(f"每日上限: {config.daily_follow_limit} | 评分阈值: {config.min_score_threshold}")

    # 初始化组件
    db = Database()
    device = Device(serial=config.device.serial)

    try:
        device.connect()
        device.launch_xhs()
        run_session(config, db, device)
    except Exception as e:
        logger.exception(f"主流程异常: {e}")
    finally:
        db.close()
        device.press_home()
        logger.info("脚本退出")


if __name__ == "__main__":
    main()
