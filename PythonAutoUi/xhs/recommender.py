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
        """完整流程入口：根据 config.mode 分发到对应路径。"""
        logger.info("进入推荐列表路径")
        self._nav.go_to_my_tab()
        self._nav.click_following_count()

        if self._cfg.mode == "my_following":
            logger.info("使用我的关注列表探索模式")
            return self._run_my_following_explore()

        self._nav.switch_to_recommend_tab()

        if self._cfg.direct_follow:
            logger.info("使用直接关注模式（不进主页）")
            return self._run_direct_follow()
        else:
            logger.info("使用主页关注模式（进主页+横向推荐）")
            return self._run_profile_follow()

    # ------------------------------------------------------------------ #
    # 路径 C：我的关注列表探索模式
    # ------------------------------------------------------------------ #

    def _run_my_following_explore(self) -> int:
        """
        路径 C：从「我的关注列表」出发，逐个进入已关注博主的主页，
        再点击其关注数进入其关注列表，从中发现并关注新的互关跑步博主。

        行为特征：
          - 优先选择昵称含互关关键词的博主（如"互关"、"有关必回"等）
          - 其余博主随机选择，不按列表顺序，模拟真人浏览
          - 每次点击前进行随机滑动浏览，偶尔回看，速度/距离随机
          - 若无法进入某博主的关注列表，静默跳过换下一个
        """
        d = self._d
        self._nav.switch_to_following_tab()

        explored: set[str] = set()
        bloggers_explored = 0
        max_bloggers = self._cfg.max_bloggers_to_explore
        scroll_count = 0
        max_scrolls = self._cfg.pages_per_recommend
        consecutive_empty = 0

        while bloggers_explored < max_bloggers and scroll_count <= max_scrolls:
            if not can_follow(self._db, self._cfg):
                logger.info("达到每日上限，停止探索")
                break

            # 每轮先做人类式随机浏览滑动（首轮除外）
            if scroll_count > 0 or bloggers_explored > 0:
                self._human_scroll_in_following_list()
            scroll_count += 1

            # 收集当前屏幕可见用户
            visible = self._collect_my_following_users()
            candidates = [u for u in visible if u not in explored]

            if not candidates:
                consecutive_empty += 1
                if consecutive_empty >= 3:
                    logger.info("我的关注列表已无新博主可探索，结束")
                    break
                continue

            consecutive_empty = 0

            # 优先级排序：昵称含互关词 → 随机
            priority = [u for u in candidates if self._has_mutual_keyword(u)]
            rest     = [u for u in candidates if not self._has_mutual_keyword(u)]
            random.shuffle(rest)
            sorted_candidates = priority + rest

            blogger = sorted_candidates[0]
            explored.add(blogger)
            bloggers_explored += 1

            tag = "（互关词命中）" if self._has_mutual_keyword(blogger) else ""
            logger.info(
                f"[我的关注列表] 探索 {bloggers_explored}/{max_bloggers}: {blogger}{tag}"
            )

            # 点击前短暂停留（模拟视线落在该条目上）
            time.sleep(random.uniform(0.3, 1.0))

            # 进入博主主页
            if not self._click_user_by_name(blogger):
                logger.debug(f"无法点击 {blogger}（可能已滚出屏幕），跳过")
                continue

            # 尝试进入其关注列表并探索（内部处理进不去的情况）
            try:
                self._explore_blogger_following_list(depth=1)
            except RateLimitWarning:
                d.press("back")
                time.sleep(0.8)
                raise
            except Exception as e:
                logger.warning(f"探索 {blogger} 关注列表异常: {e}")
                d.press("back")
                time.sleep(0.8)

            # 从博主主页返回我的关注列表
            d.press("back")
            time.sleep(random.uniform(1.0, 1.8))

            # 验证是否成功回到关注列表页
            if not d(text="推荐").wait(timeout=5):
                logger.warning("未检测到推荐 Tab，重新导航到我的关注列表")
                try:
                    self._nav.go_to_my_tab()
                    self._nav.click_following_count()
                    self._nav.switch_to_following_tab()
                    scroll_count = 0  # 重置滚动计数
                except Exception as nav_e:
                    logger.error(f"重新导航失败，停止探索: {nav_e}")
                    break

            self._delay.between_profiles()

        return self._follows_this_session

    def _has_mutual_keyword(self, username: str) -> bool:
        """判断昵称是否含有互关意向词（不区分大小写）。"""
        lower = username.lower()
        return any(kw.lower() in lower for kw in self._MUTUAL_INTENT)

    def _human_scroll_in_following_list(self) -> None:
        """
        模拟人类在关注列表中的浏览滑动：
          - 随机 1-3 次向上滑动（浏览更多）
          - 偶尔夹杂短距离向下回看（约 25% 概率）
          - 每次滑动距离和停顿时间均随机
        """
        swipes = random.randint(1, 3)
        for i in range(swipes):
            # 偶尔向下回划（看上方内容）
            if i > 0 and random.random() < 0.25:
                back_dist = random.randint(80, 220)
                self._d.swipe(
                    540, 1000,
                    540, 1000 + back_dist,
                    duration=random.uniform(0.25, 0.55),
                )
                time.sleep(random.uniform(0.4, 1.2))

            dist = random.randint(280, 680)
            human_swipe_up(self._d, distance=dist)
            time.sleep(random.uniform(0.8, 2.5))

    def _collect_my_following_users(self) -> List[str]:
        """
        从我的关注列表（'关注' Tab）收集当前可见的用户名。

        我的关注列表与推荐列表结构相似，区别：
          - 右侧按钮为「已关注」或「互相关注」（不是「关注」）
          - 可复用 _find_username_near_button() 定位同行用户名
        """
        usernames: List[str] = []
        try:
            xml_str = self._d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            follow_btns = []
            for node in root.iter():
                t = node.get("text", "").strip()
                bounds = node.get("bounds", "")
                cls = node.get("class", "")
                # 已关注/互相关注按钮在右侧（x > 700）
                if t in ("已关注", "互相关注") and "TextView" in cls and bounds:
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
            logger.warning(f"我的关注列表用户名收集失败: {e}")

        logger.debug(f"我的关注列表当前可见: {len(usernames)} 个用户")
        return usernames

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
                try:
                    human_swipe_up(self._d)
                except Exception as swipe_err:
                    # ATX 断线时向上层抛出，由 main.py 的重连逻辑处理
                    raise swipe_err
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
                # 用用户名重新定位按钮（避免旧坐标失效），点击后用用户名验证
                success = self._click_follow_button_for_user(username)
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

    def _click_follow_button_for_user(self, username: str) -> bool:
        """
        以用户名为锚点重新扫描 UI，精确定位该用户的'关注'按钮并点击，
        点击后再次 dump 以用户名锚点验证按钮状态变化。
        相比旧的 stale-bounds 方案，从根本上解决坐标过期和误判问题。
        """
        d = self._d
        try:
            # 1. 重新 dump，用用户名找到其对应的'关注'按钮（最新坐标）
            xml_fresh = d.dump_hierarchy()
            root_fresh = ET.fromstring(xml_fresh)
            btn_bounds = self._find_follow_btn_for_username(root_fresh, username)
            if not btn_bounds:
                logger.debug(f"未找到 {username} 的'关注'按钮（已关注/不在视图）")
                return False

            l, top, r, bot = self._parse_bounds(btn_bounds)
            cx = (l + r) // 2 + random.randint(-8, 8)
            cy = (top + bot) // 2 + random.randint(-5, 5)
            logger.debug(f"点击 {username} 的关注按钮: ({cx},{cy})")

            self._delay.tap()
            d.click(cx, cy)
            time.sleep(1.5)

            # 2. 重新 dump，确认该用户行的按钮文字已变为"已关注"/"互相关注"
            xml_after = d.dump_hierarchy()
            root_after = ET.fromstring(xml_after)
            if self._verify_followed_for_user(root_after, username):
                return True

            # 3. 如果文字匹配失败，检查该用户行是否已无"关注"按钮（某些版本直接消失）
            if self._find_follow_btn_for_username(root_after, username) is None:
                logger.debug(f"{username} 行关注按钮已消失，视为成功")
                return True

            logger.debug(f"关注验证失败: {username}（按钮未变化）")
            return False

        except Exception as e:
            logger.debug(f"_click_follow_button_for_user 异常: {e}")
            return False

    def _find_follow_btn_for_username(
        self, root, username: str
    ) -> Optional[str]:
        """
        在 UI 树中，先定位用户名节点的 y 区间，
        再在附近（±150px）找 x>700 的 text='关注' 节点，返回其 bounds。
        """
        user_top: Optional[int] = None
        user_bot: Optional[int] = None
        for node in root.iter():
            if node.get("text", "").strip() == username:
                b = node.get("bounds", "")
                if b:
                    try:
                        _, t, _, bt = self._parse_bounds(b)
                        user_top, user_bot = t, bt
                        break
                    except Exception:
                        pass
        if user_top is None:
            return None

        for node in root.iter():
            if node.get("text", "").strip() == "关注":
                b = node.get("bounds", "")
                if not b:
                    continue
                try:
                    l, t, r, bt = self._parse_bounds(b)
                    if l > 700 and t < user_bot + 150 and bt > user_top - 150:
                        return b
                except Exception:
                    pass
        return None

    def _verify_followed_for_user(self, root_after, username: str) -> bool:
        """
        以用户名为锚点：先找用户名节点 y 区间，
        再检查该区间附近（±200px）是否出现 x>700 的"已关注"/"互相关注"。
        """
        user_top: Optional[int] = None
        user_bot: Optional[int] = None
        for node in root_after.iter():
            if node.get("text", "").strip() == username:
                b = node.get("bounds", "")
                if b:
                    try:
                        _, t, _, bt = self._parse_bounds(b)
                        user_top, user_bot = t, bt
                        break
                    except Exception:
                        pass

        success_texts = {"已关注", "互相关注"}
        for node in root_after.iter():
            node_text = node.get("text", "").strip()
            if node_text in success_texts:
                b = node.get("bounds", "")
                if b:
                    try:
                        l, node_top, r, node_bot = self._parse_bounds(b)
                        if l > 700:
                            # 如果找到了用户名节点就精确匹配，否则宽松匹配
                            if user_top is not None:
                                if node_top < user_bot + 200 and node_bot > user_top - 200:
                                    return True
                            else:
                                return True  # 找不到用户名节点则任意"已关注"都算
                    except Exception:
                        pass
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
                if scroll_count >= max_scrolls:
                    logger.info("已达翻页上限，停止")
                    break
                # 连续 2 轮无新用户：先去首页随机浏览，再回来刷新推荐列表
                if empty_rounds >= 2:
                    logger.info(f"推荐列表连续 {empty_rounds} 轮为空，去首页随机浏览后回来")
                    self._browse_home_and_return()
                    empty_rounds = 0   # 重置计数，给推荐列表刷新机会
                    scroll_count += 1
                    continue
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

        # 双重判定：必须同时是跑步博主 + 互关类型
        is_runner, is_mutual = self._is_runner_and_mutual(profile)
        if not is_runner:
            logger.debug(f"非跑步博主，跳过: {username}（bio={profile.bio_text[:30]!r}）")
            d.press("back")
            time.sleep(0.8)
            return False
        if not is_mutual:
            logger.debug(f"非互关类型，跳过: {username}（粉丝={profile.followers} 关注={profile.following}）")
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
            # ── 进入博主关注列表探索（评分通过后、关注前）────────────────────
            if self._cfg.explore_following_list and depth == 1:
                self._explore_blogger_following_list(depth)

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
    # 博主关注列表探索（可选路径）
    # ------------------------------------------------------------------ #

    def _explore_blogger_following_list(self, depth: int) -> None:
        """
        在博主主页点击「关注数」区域，进入其关注列表探索更多互关跑步博主。

        流程：
          1. 调用 navigator.click_blogger_following_count() 进入列表
          2. 若无法进入（私密账户等），直接返回，不影响后续关注流程
          3. 若进入成功，收集列表中的用户名并逐一调用 _process_user
          4. 最后 press back 回到博主主页（继续后续关注/横向推荐逻辑）

        只在 depth==1 时触发，防止递归膨胀。
        """
        d = self._d
        logger.info("尝试进入博主关注列表探索...")

        entered = self._nav.click_blogger_following_count()
        if not entered:
            logger.debug("未能进入博主关注列表，跳过")
            return

        try:
            users = self._collect_following_list_users()
            if not users:
                logger.debug("博主关注列表无可探索用户（可能为空或受限）")
                return

            limit = min(len(users), self._cfg.max_following_list_users)
            logger.info(f"博主关注列表探索：发现 {len(users)} 人，处理前 {limit} 人")

            for username in users[:limit]:
                if not can_follow(self._db, self._cfg):
                    break
                if self._db.is_already_processed(username):
                    continue
                if random.random() < self._cfg.random_skip_ratio:
                    continue

                logger.info(f"[关注列表探索] 处理: {username}")
                try:
                    followed = self._process_user(username, depth=depth + 1)
                    if followed:
                        self._follows_this_session += 1
                except RateLimitWarning:
                    raise
                except Exception as e:
                    logger.warning(f"关注列表探索 {username} 异常: {e}")
                    d.press("back")
                    time.sleep(0.8)

                self._delay.between_profiles()

        except RateLimitWarning:
            raise
        except Exception as e:
            logger.warning(f"博主关注列表探索异常: {e}")
        finally:
            # 无论成功与否，按 back 返回博主主页
            d.press("back")
            time.sleep(1.0)
            logger.debug("已从博主关注列表返回主页")

    def _collect_following_list_users(self) -> List[str]:
        """
        从博主的关注列表页收集用户名。

        关注列表与推荐列表 UI 结构相似：
          - 右侧有「关注」按钮（未关注时 x > 700）
          - 用户名 TextView 在按钮同行左侧（x 区间 [80, 729]）
        复用 _collect_recommend_usernames / _find_username_near_button 逻辑。
        """
        usernames: List[str] = []
        try:
            xml_str = self._d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            follow_btns = []
            for node in root.iter():
                t = node.get("text", "").strip()
                bounds = node.get("bounds", "")
                cls = node.get("class", "")
                if t in ("关注", "+ 关注", "+关注") and "TextView" in cls and bounds:
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
            logger.warning(f"博主关注列表用户名收集失败: {e}")

        logger.debug(f"博主关注列表收集到 {len(usernames)} 个用户名")
        return usernames

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

        实机验证结构（debug_horiz.py）：
          - 标题 text='关注他也关注①' at y≈1211
          - 用户名 TextView 横向分布 at y≈title_y+270（如 y≈1484）
          - 粉丝数文本 at y≈title_y+330
          - text='关注' TextView at y≈title_y+410，clickable=false
          → 必须点用户名进入主页才能关注，不能直接点列表中的"关注"
        """
        import re as _re

        d = self._d
        usernames: List[str] = []
        seen: set[str] = set()

        _EXCLUDE = {"关注", "已关注", "互相关注", "粉丝", "关注他"}
        _EXCLUDE_CONTAINS = [
            "也关注", "他们也关注", "共同关注", "共同好友", "的人也关注", "关注他",
        ]
        # 数字/单位组成的粉丝数文本，如 "4152"、"1.2万"、"300"
        _NUM_PATTERN = _re.compile(r'^[\d\.万千百kKmM\+]+$')

        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            # ── 1. 找"也关注"标题节点，获取其 y 坐标 ────────────────────────
            title_y: Optional[int] = None
            for node in root.iter():
                t = node.get("text", "").strip()
                if "也关注" in t:
                    b = node.get("bounds", "")
                    if b:
                        try:
                            _, top, _, _ = self._parse_bounds(b)
                            title_y = top
                            logger.debug(f"横向推荐标题: {t!r} at y={top}")
                            break
                        except Exception:
                            pass

            if title_y is None:
                logger.debug("未找到横向推荐标题（'也关注'），跳过横向推荐")
                return []

            # ── 2. 在标题下方 [title_y+80, title_y+500] 找用户名 TextView ───
            #    用户名横向分布（x 不限），需排除：数字、固定词、含"也关注"等
            range_top = title_y + 80
            range_bot = title_y + 500

            candidates: List[tuple[int, str]] = []  # (x, name)
            for node in root.iter():
                t = node.get("text", "").strip()
                cls = node.get("class", "")
                b = node.get("bounds", "")
                if not (t and "TextView" in cls and b):
                    continue
                if t in _EXCLUDE:
                    continue
                if any(kw in t for kw in _EXCLUDE_CONTAINS):
                    continue
                if not (1 < len(t) <= 20):
                    continue
                if _NUM_PATTERN.match(t):
                    continue
                try:
                    l, top, r, _ = self._parse_bounds(b)
                    if range_top <= top <= range_bot:
                        candidates.append((l, t))
                except Exception:
                    pass

            # 按 x 坐标从左到右排序（横向卡片顺序），去重
            candidates.sort(key=lambda x: x[0])
            for x_pos, name in candidates:
                if name not in seen:
                    seen.add(name)
                    usernames.append(name)
                    logger.debug(f"横向推荐用户: {name!r} at x={x_pos}")

        except Exception as e:
            logger.debug(f"横向推荐解析失败: {e}")

        return usernames

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
    # 跑友 + 互关类型双重判定
    # ------------------------------------------------------------------ #

    # ── 跑步强关键词：出现任意1个即确认为跑步相关 ──────────────────────────
    _RUNNER_STRONG = [
        "跑步", "奔跑", "马拉松", "越野跑", "越野", "跑龄", "配速", "半马", "全马",
        "trail", "itra", "跑量", "跑者", "跑友", "跑圈", "晨跑", "夜跑", "长跑",
        "跑马", "全程马", "破三", "破四", "sub3", "sub4", "跑团", "跑club",
    ]
    # ── 跑步弱关键词：需出现 2+ 个 ─────────────────────────────────────────
    _RUNNER_WEAK = [
        "run", "runner", "running", "公里", "km", "pb", "备赛", "5k", "10k",
        "健步", "慢跑", "冲关", "打卡", "训练",
    ]
    # ── 互关意向关键词 ──────────────────────────────────────────────────────
    _MUTUAL_INTENT = [
        "互关", "跑互", "跑友互关", "回关", "有关必回", "必回", "一定回",
        "互粉", "互fo", "(互)", "【互】",
    ]

    def _is_runner_and_mutual(self, profile) -> tuple[bool, bool]:
        """
        基于 profile.bio_text / profile.social_proof_text / username 判断：
          1. 是否是跑步博主（runner）
          2. 是否是互关类型（mutual-type）

        只扫描：昵称、简介、社交证明文字（不扫全页，避免误判）。
        返回 (is_runner, is_mutual)。
        """
        username = profile.username or ""
        bio = profile.bio_text or ""
        social = profile.social_proof_text or ""

        # 合并三个来源为检测语料
        corpus = (username + "\n" + bio + "\n" + social).lower()

        # ── 跑步判定 ──────────────────────────────────────────────────────
        is_runner = False
        # 强关键词：任意1个
        for kw in self._RUNNER_STRONG:
            if kw.lower() in corpus:
                logger.debug(f"跑步强关键词命中: '{kw}'")
                is_runner = True
                break
        # 弱关键词：2+ 个
        if not is_runner:
            weak_hits = [kw for kw in self._RUNNER_WEAK if kw.lower() in corpus]
            if len(weak_hits) >= 2:
                logger.debug(f"跑步弱关键词命中 {len(weak_hits)} 个: {weak_hits}")
                is_runner = True

        # ── 互关类型判定 ──────────────────────────────────────────────────
        is_mutual = False
        # 优先：昵称/简介/社交证明含互关关键词
        for kw in self._MUTUAL_INTENT:
            if kw.lower() in corpus:
                logger.debug(f"互关关键词命中: '{kw}'")
                is_mutual = True
                break
        # 备用（无关键词时）：关注数 > 粉丝数 且 关注数 >= 300
        # 说明该用户在主动建立互关网络，回关概率较高
        if not is_mutual:
            fo, f = profile.following, profile.followers
            if fo > f and fo >= 300:
                logger.debug(
                    f"互关兜底通过（关注>粉丝且关注≥1000）: "
                    f"粉丝={f} 关注={fo} ratio={fo/max(f,1):.2f}"
                )
                is_mutual = True

        return is_runner, is_mutual

    def _click_user_by_name(self, username: str) -> bool:
        """
        在当前页面点击指定用户名进入主页。
        等待条件：关注 Button 出现（className=android.widget.Button，text=关注/已关注/互相关注）。
        注意：不能用 text='粉丝' 作等待条件，推荐列表顶部的"粉丝" Tab 会立即误判。
        """
        d = self._d
        node = d(text=username)
        if not node.exists:
            logger.debug(f"未找到用户节点: {username}")
            return False
        node.click()
        time.sleep(1.5)
        # 博主主页特有：关注/已关注/互相关注 Button（推荐列表里这些是 TextView）
        for follow_text in ("关注", "已关注", "互相关注"):
            if d(text=follow_text, className="android.widget.Button").wait(timeout=6):
                return True
        logger.warning(f"进入 {username} 主页超时（未检测到关注 Button）")
        d.press("back")
        return False

    def _safe_back_to_recommend(self) -> None:
        """安全返回推荐列表（连续 press_back 直到推荐 Tab 可见）。"""
        for _ in range(6):
            # text='推荐' 是推荐 Tab（实机验证，contentDescription 为空）
            if self._d(text="推荐").exists:
                break
            self._d.press("back")
            time.sleep(0.8)

    def _browse_home_and_return(self) -> None:
        """
        模拟真人行为：离开推荐列表 → 去首页随机浏览其他 Tab → 回来重新导航到推荐列表。
        触发时机：推荐列表连续多轮无新用户（列表已刷新耗尽）。
        """
        d = self._d

        # ── 1. 去到首页 Tab（text="首页"，contentDescription 为空）─────────
        try:
            if d(text="首页").exists:
                d(text="首页").click()
            time.sleep(random.uniform(1.5, 2.5))
            logger.debug("已切换到首页，开始随机浏览")
        except Exception as e:
            logger.debug(f"切换首页失败: {e}")
            return

        # ── 2. 在首页上下滑动若干次，模拟刷推荐 Feed ─────────────────────
        swipe_count = random.randint(3, 6)
        for i in range(swipe_count):
            try:
                human_swipe_up(d, distance=random.randint(800, 1400))
                time.sleep(random.uniform(1.2, 3.0))
            except Exception:
                break

        # ── 3. 随机 30% 概率点击其他 Tab（市集/消息），再返回 ──────────
        # 实机验证：底部 Tab 为 首页/市集/消息/我，均用 text 属性
        other_tabs = ["市集", "消息"]
        if random.random() < 0.30:
            tab_name = random.choice(other_tabs)
            try:
                if d(text=tab_name).exists:
                    d(text=tab_name).click()
                else:
                    tab_name = None
                if tab_name:
                    logger.debug(f"随机切换到 {tab_name} Tab")
                    time.sleep(random.uniform(2.0, 4.0))
                    # 在该 Tab 随机上下滑动 1-3 次
                    for _ in range(random.randint(1, 3)):
                        human_swipe_up(d, distance=random.randint(600, 1000))
                        time.sleep(random.uniform(1.0, 2.5))
            except Exception as e:
                logger.debug(f"随机 Tab 浏览失败: {e}")

        # ── 4. 重新导航回推荐列表（最多重试 2 次） ───────────────────────
        logger.info("随机浏览完毕，重新导航回推荐列表")
        for attempt in range(3):
            try:
                self._nav.go_to_my_tab()
                self._nav.click_following_count()
                self._nav.switch_to_recommend_tab()
                logger.debug("已成功重新导航到推荐列表")
                break
            except Exception as e:
                logger.warning(f"重新导航到推荐列表失败（第{attempt+1}次）: {e}")
                # 每次重试前先 press back 清理现场，再多等一会儿
                for _ in range(4):
                    d.press("back")
                    time.sleep(1.0)
                time.sleep(2.0)
                if attempt == 2:
                    logger.warning("重新导航最终失败，将在下轮主循环继续尝试")

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
