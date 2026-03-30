"""
推荐列表路径模块（v2）

两种关注路径：
  A. 直接关注（快速）：在推荐列表里直接点击"关注"按钮，不进入主页。
     → 页面停留在推荐列表，速度快，不做评分过滤。
     → 配置 direct_follow: true 时使用此路径。

  B. 主页关注（过滤）：点击用户名进入主页，解析数据，评分后关注。
     → 关注后页面下方出现横向"关注他的人也关注的"列表，继续处理。
     → 配置 direct_follow: false 时使用此路径（默认）。

导航流程（共同）：
  我的 Tab → 关注数量按钮 → 推荐 Tab → 主循环
"""

from __future__ import annotations

import random
import time
import xml.etree.ElementTree as ET
from typing import TYPE_CHECKING, List, Optional

import uiautomator2 as u2
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_fixed

from anti_detection.gesture import human_swipe_up, human_swipe_left
from anti_detection.human_delay import HumanDelay
from core.navigator import Navigator
from persistence.database import Database
from persistence.models import BloggerProfile
from strategy.daily_limit import can_follow
from strategy.scoring import compute_follow_score
from utils.cn_number import parse_cn_number
from xhs.follow_action import FollowAction, RateLimitWarning
from xhs.profile_parser import ProfileParser

if TYPE_CHECKING:
    from utils.config_loader import AppConfig


class Recommender:
    """推荐列表路径自动关注器。"""

    def __init__(
        self,
        d: u2.Device,
        navigator: Navigator,
        delay: HumanDelay,
        parser: ProfileParser,
        follower: FollowAction,
        db: Database,
        config: "AppConfig",
        session_id: int,
    ):
        self._d = d
        self._nav = navigator
        self._delay = delay
        self._parser = parser
        self._follower = follower
        self._db = db
        self._cfg = config
        self._session_id = session_id
        self._follows_this_session = 0

    # ------------------------------------------------------------------ #
    # 公共入口
    # ------------------------------------------------------------------ #

    def run(self) -> int:
        """完整流程入口：导航到推荐列表，遍历并关注。"""
        logger.info("进入推荐列表路径")
        self._nav.go_to_my_tab()
        self._nav.click_following_count()
        self._nav.switch_to_recommend_tab()

        if self._cfg.direct_follow:
            logger.info("使用直接关注模式（不进主页）")
            return self._run_direct_follow()
        else:
            logger.info("使用主页关注模式（进主页+横向推荐）")
            return self._run_profile_follow()

    # ------------------------------------------------------------------ #
    # 路径 A：直接关注模式（推荐列表直接点按钮）
    # ------------------------------------------------------------------ #

    def _run_direct_follow(self) -> int:
        """
        直接在推荐列表点击"关注"按钮，不进入主页。
        每次处理一个按钮，处理后重新扫描可见按钮。
        """
        max_scrolls = self._cfg.pages_per_recommend
        processed_positions: set[str] = set()  # 已点击的按钮坐标标识
        scroll_count = 0
        empty_rounds = 0

        while scroll_count <= max_scrolls:
            if not can_follow(self._db, self._cfg):
                logger.info("每日关注上限已达，停止")
                break

            if self._follows_this_session >= self._cfg.max_follows_per_session:
                logger.info(f"单次会话上限，休息")
                self._delay.session_break(self._cfg.session_break_seconds)
                self._follows_this_session = 0

            # 收集当前可见的未处理 (username, btn_bounds) 对
            cards = self._collect_recommend_cards()
            new_cards = [
                (name, btn) for name, btn in cards
                if name not in processed_positions
                and not self._db.is_already_processed(name)
            ]

            if not new_cards:
                empty_rounds += 1
                if empty_rounds >= 3 or scroll_count >= max_scrolls:
                    logger.info("无新卡片或已达翻页上限，停止")
                    break
                logger.debug(f"无新卡片，第 {scroll_count + 1} 次滚动")
                human_swipe_up(self._d)
                self._delay.scroll_pause()
                scroll_count += 1
                continue

            empty_rounds = 0
            username, btn_bounds = new_cards[0]
            processed_positions.add(username)

            if random.random() < self._cfg.random_skip_ratio:
                logger.debug(f"随机跳过: {username}")
                self._delay.tap()
                continue

            # 保存到数据库（不做评分，直接关注）
            profile = BloggerProfile(username=username, follow_score=0)
            self._db.save_blogger(profile)

            # 点击关注按钮
            if self._cfg.dry_run:
                logger.info(f"[DRY RUN] 直接关注: {username}")
                self._db.record_follow(username, self._session_id, confirmed=True)
                self._follows_this_session += 1
            else:
                success = self._click_follow_button_at(btn_bounds)
                if success:
                    logger.success(f"直接关注成功（已验证UI变化）: {username}")
                    self._db.record_follow(username, self._session_id, confirmed=True)
                    self._follows_this_session += 1
                    self._delay.post_follow()
                    # 检测平台限速警告
                    self._check_rate_limit()
                else:
                    logger.warning(f"关注未成功（UI未变化），跳过: {username}")

            self._delay.between_profiles()

        return self._follows_this_session

    def _collect_recommend_cards(self) -> List[tuple[str, str]]:
        """
        从推荐列表收集 (用户名, 关注按钮bounds) 对。
        返回当前屏幕可见的卡片列表。
        """
        result = []
        try:
            xml_str = self._d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            # 找所有右侧"关注"按钮（x > 700，TextView）
            follow_btns = []
            for node in root.iter():
                t = node.get("text", "").strip()
                bounds = node.get("bounds", "")
                cls = node.get("class", "")
                if t == "关注" and "TextView" in cls and bounds:
                    try:
                        l, top, r, bot = self._parse_bounds(bounds)
                        if l > 700:
                            follow_btns.append((top, bot, bounds))
                    except Exception:
                        pass

            # 对每个按钮，找同 y 区间内的用户名
            for btn_top, btn_bot, btn_bounds in follow_btns:
                username = self._find_username_near_button(root, btn_top, btn_bot)
                if username:
                    result.append((username, btn_bounds))

        except Exception as e:
            logger.warning(f"收集推荐卡片失败: {e}")
        return result

    def _click_follow_button_at(self, bounds: str) -> bool:
        """根据 bounds 字符串点击关注按钮，并验证按钮状态是否变化。"""
        try:
            l, top, r, bot = self._parse_bounds(bounds)
            cx = (l + r) // 2 + random.randint(-8, 8)
            cy = (top + bot) // 2 + random.randint(-5, 5)
            self._delay.tap()
            self._d.click(cx, cy)
            time.sleep(1.5)
            # 验证按钮是否已变为成功状态
            if self._verify_card_button_changed(top, bot):
                return True
            logger.debug(f"坐标点击后未检测到状态变化，尝试元素兜底点击")
            return self._fallback_element_click(top, bot)
        except Exception as e:
            logger.debug(f"按钮点击失败: {e}")
            return False

    def _verify_card_button_changed(self, btn_top: int, btn_bot: int) -> bool:
        """重新 dump hierarchy，验证对应行的按钮是否已变为成功状态。"""
        success_texts = {"已关注", "互相关注"}
        try:
            xml_str = self._d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            for node in root.iter():
                t = node.get("text", "").strip()
                bounds = node.get("bounds", "")
                if t in success_texts and bounds:
                    _, top, _, _ = self._parse_bounds(bounds)
                    # 在同一行附近（±120px 容差，适配不同屏幕密度）
                    if abs(top - btn_top) < 120:
                        return True
        except Exception as e:
            logger.debug(f"验证按钮状态失败: {e}")
        return False

    def _fallback_element_click(self, btn_top: int, btn_bot: int) -> bool:
        """
        兜底方案：通过元素查找点击最接近目标位置的"关注"按钮。
        适用于坐标偏移导致坐标点击失效的情况。
        """
        d = self._d
        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            # 找所有右侧"关注"按钮，取 y 坐标最接近目标行的那个
            best_bounds = None
            best_dist = float("inf")
            for node in root.iter():
                t = node.get("text", "").strip()
                bounds = node.get("bounds", "")
                cls = node.get("class", "")
                if t == "关注" and "TextView" in cls and bounds:
                    try:
                        l, top, r, bot = self._parse_bounds(bounds)
                        if l > 700:
                            dist = abs(top - btn_top)
                            if dist < best_dist:
                                best_dist = dist
                                best_bounds = (l, top, r, bot)
                    except Exception:
                        pass

            if best_bounds and best_dist < 200:
                l, top, r, bot = best_bounds
                cx = (l + r) // 2 + random.randint(-5, 5)
                cy = (top + bot) // 2 + random.randint(-3, 3)
                logger.debug(f"兜底元素点击: ({cx}, {cy})")
                d.click(cx, cy)
                time.sleep(1.5)
                return self._verify_card_button_changed(top, bot)
        except Exception as e:
            logger.debug(f"兜底点击失败: {e}")
        return False

    def _check_rate_limit(self) -> None:
        """检测平台限速弹窗。"""
        d = self._d
        for text in ["关注了太多", "操作过于频繁", "请稍后再试"]:
            if d(textContains=text).exists:
                if d(text="确定").exists:
                    d(text="确定").click()
                elif d(text="我知道了").exists:
                    d(text="我知道了").click()
                raise RateLimitWarning(f"平台限制: {text}")

    # ------------------------------------------------------------------ #
    # 路径 B：主页关注模式（进主页 + 横向推荐）
    # ------------------------------------------------------------------ #

    def _run_profile_follow(self) -> int:
        """
        进入用户主页，评分后关注，关注后处理横向推荐列表。
        """
        max_scrolls = self._cfg.pages_per_recommend
        processed_all: set[str] = set()
        scroll_count = 0
        empty_rounds = 0

        while scroll_count <= max_scrolls:
            if not can_follow(self._db, self._cfg):
                logger.info("每日关注上限已达，停止")
                break

            if self._follows_this_session >= self._cfg.max_follows_per_session:
                logger.info(f"单次会话上限，休息")
                self._delay.session_break(self._cfg.session_break_seconds)
                self._follows_this_session = 0

            visible_users = self._collect_recommend_usernames()
            new_users = [
                u for u in visible_users
                if u not in processed_all
                and not self._db.is_already_processed(u)
            ]

            if not new_users:
                empty_rounds += 1
                if empty_rounds >= 3 or scroll_count >= max_scrolls:
                    logger.info("无新用户或已达翻页上限，停止")
                    break
                logger.debug(f"无新用户，第 {scroll_count + 1} 次滚动")
                human_swipe_up(self._d)
                self._delay.scroll_pause()
                scroll_count += 1
                continue

            empty_rounds = 0
            username = new_users[0]
            processed_all.add(username)

            if random.random() < self._cfg.random_skip_ratio:
                logger.debug(f"随机跳过: {username}")
                self._delay.between_profiles()
                continue

            logger.info(f"[推荐列表] 处理: {username}（已关注 {self._follows_this_session}）")

            try:
                followed = self._process_user(username, depth=1)
                if followed:
                    self._follows_this_session += 1
            except RateLimitWarning:
                raise
            except Exception as e:
                logger.warning(f"处理 {username} 异常: {e}")
                self._safe_back_to_recommend()

            self._delay.between_profiles()

        return self._follows_this_session

    # ------------------------------------------------------------------ #
    # 单用户处理（路径 B）
    # ------------------------------------------------------------------ #

    def _process_user(self, username: str, depth: int) -> bool:
        """
        点击用户名进入主页 → 解析 → 评分 → 关注 → 横向推荐（深度限制）。
        返回 True 表示成功关注。
        """
        d = self._d

        if not self._click_user_by_name(username):
            return False

        profile = self._parser.parse(username)
        if profile is None:
            d.press("back")
            time.sleep(0.8)
            return False

        score = compute_follow_score(profile)
        profile.follow_score = score
        self._db.save_blogger(profile)

        if score < 0:
            logger.debug(f"硬过滤 {username} (score={score})")
            d.press("back")
            time.sleep(0.8)
            return False

        if score < self._cfg.min_score_threshold:
            logger.debug(f"评分不足 {username} ({score}<{self._cfg.min_score_threshold})")
            d.press("back")
            time.sleep(0.8)
            return False

        followed = False
        try:
            followed = self._follower.follow(profile, dry_run=self._cfg.dry_run)
            if followed:
                self._db.record_follow(username, self._session_id, confirmed=True)
                self._delay.post_follow()
        except RateLimitWarning:
            d.press("back")
            raise

        # 关注成功 → 处理横向推荐
        if followed and depth < self._cfg.max_depth:
            self._process_horizontal_recommendations(depth)

        d.press("back")
        time.sleep(0.8)
        return followed

    # ------------------------------------------------------------------ #
    # 横向推荐处理（关注后出现）
    # ------------------------------------------------------------------ #

    def _process_horizontal_recommendations(self, current_depth: int) -> None:
        """
        关注成功后，等待并处理页面下方横向推荐列表。
        「关注他的人也关注的」水平滑动列表，最多处理 3 个用户。
        """
        d = self._d
        time.sleep(2.0)

        # 向下滚动，让横向推荐区域进入视野
        human_swipe_up(self._d, distance=600)
        time.sleep(1.5)

        recs = self._get_horizontal_recommendations()
        if not recs:
            logger.debug("未检测到横向推荐列表")
            return

        logger.info(f"横向推荐 {len(recs)} 人（深度 {current_depth}）")

        for username in recs[:3]:
            if not can_follow(self._db, self._cfg):
                break
            if self._db.is_already_processed(username):
                continue
            if random.random() < self._cfg.random_skip_ratio:
                continue

            try:
                followed = self._process_user(username, depth=current_depth + 1)
                if followed:
                    self._follows_this_session += 1
            except RateLimitWarning:
                raise
            except Exception as e:
                logger.warning(f"横向推荐处理异常 {username}: {e}")
                d.press("back")
                time.sleep(0.8)

            self._delay.between_profiles()

    def _get_horizontal_recommendations(self) -> List[str]:
        """
        解析横向推荐列表（关注后出现在主页底部）。
        特征：包含多个小型用户卡片的水平可滑动容器。
        """
        d = self._d
        usernames = []
        seen: set[str] = set()

        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            # 策略1：找 HorizontalScrollView / RecyclerView 中包含"关注"按钮的
            for node in root.iter():
                cls = node.get("class", "")
                if "HorizontalScrollView" in cls or (
                    "RecyclerView" in cls and self._is_horizontal(node)
                ):
                    inner_texts = [n.get("text", "") for n in node.iter()]
                    if "关注" in inner_texts or "已关注" in inner_texts:
                        for child in node.iter():
                            t = child.get("text", "").strip()
                            if (
                                t
                                and t not in ("关注", "已关注", "互相关注")
                                and 1 < len(t) <= 20
                                and t not in seen
                            ):
                                seen.add(t)
                                usernames.append(t)

            # 策略2：找"关注他的人也关注"文字附近的用户名节点
            if not usernames:
                for node in root.iter():
                    t = node.get("text", "").strip()
                    if "也关注" in t or "他们也关注" in t:
                        parent = self._find_parent(root, node)
                        if parent:
                            grandparent = self._find_parent(root, parent)
                            if grandparent:
                                for n in grandparent.iter():
                                    nt = n.get("text", "").strip()
                                    if (
                                        nt
                                        and nt not in ("关注", "已关注", "互相关注")
                                        and "也关注" not in nt
                                        and 1 < len(nt) <= 20
                                        and nt not in seen
                                    ):
                                        seen.add(nt)
                                        usernames.append(nt)

        except Exception as e:
            logger.debug(f"横向推荐解析失败: {e}")

        return usernames

    @staticmethod
    def _is_horizontal(node) -> bool:
        """判断 RecyclerView 是否是水平方向（横向推荐列表特征）。"""
        bounds = node.get("bounds", "")
        if not bounds:
            return False
        try:
            l, t, r, b = Recommender._parse_bounds(bounds)
            width = r - l
            height = b - t
            return width > height * 2  # 宽远大于高 → 横向
        except Exception:
            return False

    # ------------------------------------------------------------------ #
    # 用户名收集（路径 B 用）
    # ------------------------------------------------------------------ #

    def _collect_recommend_usernames(self) -> List[str]:
        """从当前推荐列表提取用户名列表（路径 B 专用）。"""
        usernames = []
        try:
            xml_str = self._d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            follow_btns = []
            for node in root.iter():
                t = node.get("text", "").strip()
                bounds = node.get("bounds", "")
                cls = node.get("class", "")
                if t == "关注" and "TextView" in cls and bounds:
                    try:
                        l, top, r, bot = self._parse_bounds(bounds)
                        if l > 700:
                            follow_btns.append((top, bot, node))
                    except Exception:
                        pass

            for btn_top, btn_bot, _ in follow_btns:
                username = self._find_username_near_button(root, btn_top, btn_bot)
                if username and username not in usernames:
                    usernames.append(username)

        except Exception as e:
            logger.warning(f"推荐列表用户名收集失败: {e}")
        return usernames

    def _find_username_near_button(
        self, root, btn_top: int, btn_bottom: int
    ) -> Optional[str]:
        """在'关注'按钮附近找用户名 TextView（x=[234,729] 范围）。"""
        candidates = []
        for node in root.iter():
            t = node.get("text", "").strip()
            cls = node.get("class", "")
            bounds = node.get("bounds", "")
            if not (t and "TextView" in cls and bounds):
                continue
            try:
                l, top, r, bot = self._parse_bounds(bounds)
                if (
                    l < 750
                    and r < 800
                    and top < btn_bottom
                    and bot > btn_top
                    and t not in ("关注", "已关注", "互相关注")
                    and "他也关注" not in t
                    and "共同关注" not in t
                    and "共同好友" not in t
                    and "的人也关注" not in t
                    and 1 < len(t) <= 20
                ):
                    candidates.append((top, t))
            except Exception:
                pass

        if candidates:
            candidates.sort(key=lambda x: x[0])
            return candidates[0][1]
        return None

    # ------------------------------------------------------------------ #
    # 导航辅助
    # ------------------------------------------------------------------ #

    def _click_user_by_name(self, username: str) -> bool:
        """在当前页面点击指定用户名进入主页。"""
        d = self._d
        node = d(text=username)
        if not node.exists:
            logger.debug(f"未找到用户节点: {username}")
            return False
        node.click()
        time.sleep(1.8)
        if not d(descriptionContains="粉丝", className="android.widget.Button").wait(timeout=8):
            logger.warning(f"进入 {username} 主页超时")
            d.press("back")
            return False
        return True

    def _safe_back_to_recommend(self) -> None:
        """安全返回推荐列表（连续 press_back 直到推荐 Tab 可见）。"""
        for _ in range(6):
            if self._d(description="推荐").exists:
                break
            self._d.press("back")
            time.sleep(0.8)

    # ------------------------------------------------------------------ #
    # 静态工具
    # ------------------------------------------------------------------ #

    @staticmethod
    def _parse_bounds(bounds: str) -> tuple[int, int, int, int]:
        """解析 '[left,top][right,bottom]' → (left, top, right, bottom)。"""
        parts = bounds.replace("][", ",").strip("[]").split(",")
        return int(parts[0]), int(parts[1]), int(parts[2]), int(parts[3])

    @staticmethod
    def _find_parent(root, target):
        for parent in root.iter():
            for child in parent:
                if child is target:
                    return parent
        return None
