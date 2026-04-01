"""
笔记流探索器：进入首页「关注」/「发现」Tab，浏览笔记瀑布流，
对跑步相关笔记随机点赞 / 收藏，完整模拟真人操作节奏。

核心流程：
  1. 导航到首页（IndexActivityV2）
  2. 按配置顺序切换 Tab（关注 → 发现）
  3. 每个 Tab 下滚动 N 页，每页随机选 1-3 张笔记点开
  4. 进入详情后：读取内容 → 判断跑步相关 → 随机点赞/收藏
  5. 返回 Feed，继续下一张

防检测措施：
  - 阅读停留时间高斯分布（min_read + 随机额外时间）
  - 点击坐标随机偏移 ±8px
  - 每页滚动后随机 scroll_pause 停顿
  - 每张笔记 between_profiles 间隔
  - like/collect 采用概率模型（非每张都互动）
  - 每日互动次数受 daily_like_limit / daily_collect_limit 控制
"""

from __future__ import annotations

import random
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from typing import Optional

import uiautomator2 as u2
from loguru import logger

from anti_detection.gesture import human_swipe_up
from anti_detection.human_delay import HumanDelay
from core.navigator import Navigator
from persistence.database import Database
from utils.config_loader import NoteExploreConfig
from xhs.note_action import NoteAction


@dataclass
class NoteCard:
    """一张笔记卡片的基本信息（从瀑布流 XML 解析）。"""
    cx: int               # 点击坐标 x
    cy: int               # 点击坐标 y
    title: str = ""       # 笔记标题（用于去重）
    width: int = 0
    height: int = 0


@dataclass
class ExploreStats:
    """本次探索会话统计。"""
    notes_seen: int = 0
    notes_running: int = 0
    likes: int = 0
    collects: int = 0
    notes_interacted: int = 0


class NoteExplorer:
    """
    笔记流探索器。

    Args:
        d:           uiautomator2 设备对象
        navigator:   Navigator 实例（用于导航回首页等）
        delay:       HumanDelay 实例
        note_action: NoteAction 实例
        db:          Database 实例（用于日限额查询）
        config:      NoteExploreConfig 实例
        session_id:  当前 RunSession ID（用于 DB 记录）
        dry_run:     True = 仅打日志，不实际点赞/收藏
    """

    def __init__(
        self,
        d: u2.Device,
        navigator: Navigator,
        delay: HumanDelay,
        note_action: NoteAction,
        db: Database,
        config: NoteExploreConfig,
        session_id: int = 0,
        dry_run: bool = False,
    ):
        self.d = d
        self.nav = navigator
        self.delay = delay
        self.action = note_action
        self.db = db
        self.cfg = config
        self.session_id = session_id
        self.dry_run = dry_run

        self._stats = ExploreStats()
        self._seen_titles: set[str] = set()   # 本次会话去重
        self._likes_this_session = 0
        self._collects_this_session = 0

    # ================================================================== #
    # 主入口
    # ================================================================== #

    def run(self) -> ExploreStats:
        """
        执行完整探索会话：遍历配置的 Tab，滚动 Feed，互动笔记。

        Returns:
            ExploreStats 统计结果。
        """
        logger.info(
            f"开始笔记探索 | tabs={self.cfg.tabs} "
            f"pages_per_tab={self.cfg.pages_per_tab} "
            f"dry_run={self.dry_run}"
        )

        # 确保在首页
        self._ensure_home()

        for tab_name in self.cfg.tabs:
            if self._is_over_daily_limit():
                logger.info("已达每日互动上限，提前结束探索")
                break
            # 每个 Tab 开始前重新确保在首页（防止多次 back 后偏离）
            self._ensure_home()
            time.sleep(random.uniform(0.8, 1.5))
            logger.info(f"── 切换到 Tab: {tab_name} ──")
            switched = self._switch_home_tab(tab_name)
            if not switched:
                logger.warning(f"Tab {tab_name!r} 切换失败，跳过")
                continue
            time.sleep(random.uniform(1.0, 2.0))
            self._explore_tab(tab_name)

        logger.info(
            f"笔记探索完成 | 浏览={self._stats.notes_seen} "
            f"跑步={self._stats.notes_running} "
            f"点赞={self._stats.likes} 收藏={self._stats.collects}"
        )
        return self._stats

    # ================================================================== #
    # Tab 导航
    # ================================================================== #

    def _ensure_home(self) -> None:
        """
        确保当前在首页（IndexActivityV2）。

        策略（按侵入性递增）：
          1. 若已在首页，直接返回
          2. 点击底部「首页」Tab（FrameLayout cd='首页'，y>2000）
          3. 若仍不在首页，最多按 3 次 back
          4. 若仍不在首页，用 am start 强制冷启动
        """

        for attempt in range(5):
            activity = self.nav.current_activity()
            if activity == "IndexActivityV2":
                return
            logger.debug(f"ensure_home attempt={attempt} activity={activity}")

            if attempt == 0:
                # 尝试点底部首页 Tab
                try:
                    import xml.etree.ElementTree as ET
                    xml_str = self.d.dump_hierarchy()
                    root = ET.fromstring(xml_str)
                    screen_h = self.d.info.get("displayHeight", 2400)
                    for node in root.iter():
                        cd = (node.get("content-desc") or "").strip()
                        b = node.get("bounds", "")
                        if cd != "首页" or not b:
                            continue
                        parts = b.replace("][", ",").strip("[]").split(",")
                        l, top, r, bot = (int(p) for p in parts)
                        if top > screen_h * 0.85:
                            self.d.click((l + r) // 2, (top + bot) // 2)
                            time.sleep(1.5)
                            break
                except Exception as e:
                    logger.debug(f"点首页Tab失败: {e}")
            elif attempt <= 3:
                self.d.press("back")
                time.sleep(1.0)
            else:
                # 最后手段：通过设备 shell 冷启动首页
                logger.warning("ensure_home: 强制 am start 回首页")
                self.d.shell(
                    "am start -n com.xingin.xhs/.index.v2.IndexActivityV2"
                )
                time.sleep(3.0)

        logger.warning("ensure_home: 未能确认回到首页")

    def _switch_home_tab(self, tab_name: str) -> bool:
        """
        切换首页顶部 Tab（关注 / 发现 / 同城）。

        IndexActivityV2 Tab 实机验证：
          - 类名: androidx.appcompat.app.ActionBar$Tab
          - 激活态: selected='true'；非激活: selected='false'
          - Tab 名称在 content-desc 属性中

        策略（按可靠性降序）：
          1. ActionBar$Tab 节点，selected='false'，cd==tab_name → 点击
          2. TextView 匹配，text==tab_name，top < 350 → 点击
          3. 若已在目标 Tab（selected='true'），直接返回 True

        Returns:
            True 表示已在（或已切换到）目标 Tab。
        """
        d = self.d

        # ── 先检查是否已在目标 Tab ──────────────────────────────────────
        if self._get_active_home_tab() == tab_name:
            logger.debug(f"已在 {tab_name!r} Tab，无需切换")
            return True

        clicked = False
        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)

            # 策略1：ActionBar$Tab 节点，cd 匹配且非激活
            for node in root.iter():
                cls = node.get("class", "")
                if "Tab" not in cls:
                    continue
                cd = (node.get("content-desc") or "").strip()
                bounds = node.get("bounds", "")
                if cd != tab_name or not bounds:
                    continue
                try:
                    parts = bounds.replace("][", ",").strip("[]").split(",")
                    l, top, r, bot = (int(p) for p in parts)
                    if top < 350:
                        d.click(
                            (l + r) // 2 + random.randint(-5, 5),
                            (top + bot) // 2 + random.randint(-4, 4),
                        )
                        clicked = True
                        logger.debug(f"Tab 切换(ActionBar$Tab cd): {tab_name!r}")
                        break
                except Exception:
                    pass

            if not clicked:
                # 策略2：TextView text 匹配
                for node in root.iter():
                    t = (node.get("text") or "").strip()
                    bounds = node.get("bounds", "")
                    if t != tab_name or not bounds:
                        continue
                    try:
                        parts = bounds.replace("][", ",").strip("[]").split(",")
                        l, top, r, bot = (int(p) for p in parts)
                        if top < 350:
                            d.click(
                                (l + r) // 2 + random.randint(-5, 5),
                                (top + bot) // 2 + random.randint(-4, 4),
                            )
                            clicked = True
                            logger.debug(f"Tab 切换(TextView): {tab_name!r}")
                            break
                    except Exception:
                        pass

        except Exception as e:
            logger.debug(f"_switch_home_tab 解析失败: {e}")

        if not clicked:
            logger.warning(f"未找到 {tab_name!r} Tab 节点")
            return False

        time.sleep(1.2)
        active = self._get_active_home_tab()
        ok = active == tab_name
        logger.debug(f"切换后激活 Tab={active!r} 目标={tab_name!r} {'✓' if ok else '✗'}")
        return ok or clicked  # clicked 表示点了，即使验证超时也继续

    def _get_active_home_tab(self) -> str:
        """
        获取首页当前激活的 Tab 名称。

        IndexActivityV2 Tab 实机验证：
          - 类名: androidx.appcompat.app.ActionBar$Tab
          - 激活态：selected='true'（与 RelationMergeActivity 的 "已选定" 前缀不同）
          - Tab 名称在 content-desc 属性中（"关注" / "发现" / "同城"）

        Returns:
            激活的 Tab 名（如 "关注"），失败返回 ""。
        """
        try:
            xml_str = self.d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            for node in root.iter():
                cls = node.get("class", "")
                if "Tab" not in cls:
                    continue
                if node.get("selected") != "true":
                    continue
                cd = (node.get("content-desc") or "").strip()
                bounds = node.get("bounds", "")
                if cd and bounds:
                    try:
                        parts = bounds.replace("][", ",").strip("[]").split(",")
                        _, top, _, _ = (int(p) for p in parts)
                        if top < 350:
                            return cd
                    except Exception:
                        pass
        except Exception as e:
            logger.debug(f"_get_active_home_tab 失败: {e}")
        return ""

    # ================================================================== #
    # Tab 内探索
    # ================================================================== #

    def _explore_tab(self, tab_name: str) -> None:
        """
        在当前 Tab 内滚动 pages_per_tab 页，逐张打开并互动笔记。
        """
        pages = 0
        consecutive_empty = 0

        while pages < self.cfg.pages_per_tab:
            if self._is_over_daily_limit():
                logger.info("每日互动限额已满，停止探索")
                return

            cards = self._collect_note_cards()
            # 过滤掉本次会话已处理的（按标题去重）
            new_cards = [c for c in cards if c.title not in self._seen_titles]

            if not new_cards:
                consecutive_empty += 1
                logger.debug(f"无新卡片（连续 {consecutive_empty} 页）")
                if consecutive_empty >= 3:
                    logger.info(f"连续 3 页无新卡片，{tab_name!r} Tab 探索结束")
                    return
            else:
                consecutive_empty = 0
                # 每页随机选 1-3 张（避免每张都互动，更真实）
                sample_n = min(len(new_cards), random.randint(1, 3))
                selected = random.sample(new_cards, sample_n)

                for card in selected:
                    if self._is_over_daily_limit():
                        return
                    self._seen_titles.add(card.title)
                    self._process_card(card)
                    # 笔记之间的间隔
                    self.delay.between_profiles()

            # 滚动到下一页
            self._scroll_feed()
            self.delay.scroll_pause()
            pages += 1

    # ================================================================== #
    # 笔记卡片收集
    # ================================================================== #

    def _collect_note_cards(self) -> list[NoteCard]:
        """
        从当前屏幕 XML 中解析可见的笔记卡片。

        XHS 瀑布流卡片特征（实机观察）：
          - 父 ViewGroup 可点击（clickable="true"）
          - 子节点含 TextView（笔记标题）
          - 卡片高度通常 > 200px，宽度约为屏幕宽的 40-50%（双列）
          - 位于 Tab 栏以下、底部导航栏以上（top > 200, bot < screen_h - 150）
          - 不包含 "已选定"、"消息"、"首页" 等导航相关文本

        Returns:
            NoteCard 列表，按 top 升序排列。
        """
        cards: list[NoteCard] = []
        try:
            xml_str = self.d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            screen_w = self.d.info.get("displayWidth", 1080)
            screen_h = self.d.info.get("displayHeight", 2400)
            min_top = 200       # Tab 栏高度约 150-200px
            max_bot = screen_h - 150  # 留出底部导航栏

            for node in root.iter():
                # 必须可点击
                if node.get("clickable") != "true":
                    continue
                cls = node.get("class", "")
                if "ViewGroup" not in cls and "FrameLayout" not in cls:
                    continue
                bounds = node.get("bounds", "")
                if not bounds:
                    continue
                try:
                    parts = bounds.replace("][", ",").strip("[]").split(",")
                    l, top, r, bot = (int(p) for p in parts)
                except Exception:
                    continue

                # 位置过滤
                if top < min_top or bot > max_bot:
                    continue
                width = r - l
                height = bot - top

                # 尺寸过滤：卡片宽度约屏宽 35-55%（双列），高度 > 200px
                if not (int(screen_w * 0.30) < width < int(screen_w * 0.60)):
                    continue
                if height < 200:
                    continue

                # 过滤视频类笔记（cd 以"视频"开头），视频详情布局不同
                card_cd = (node.get("content-desc") or "").strip()
                if card_cd.startswith("视频"):
                    continue

                # 提取标题：找子 TextView 中文本最长的（通常是笔记标题）
                title = self._extract_card_title(node)
                if not title:
                    continue

                cx = (l + r) // 2
                cy = (top + bot) // 2
                cards.append(NoteCard(cx=cx, cy=cy, title=title, width=width, height=height))

        except Exception as e:
            logger.debug(f"_collect_note_cards 解析失败: {e}")

        # 去重（同一卡片可能被多个父节点匹配到）
        seen_xy: set[tuple[int, int]] = set()
        unique: list[NoteCard] = []
        for c in sorted(cards, key=lambda x: (x.cy, x.cx)):
            key = (c.cx // 50, c.cy // 50)   # 50px 网格去重
            if key not in seen_xy:
                seen_xy.add(key)
                unique.append(c)

        logger.debug(f"收集到 {len(unique)} 张笔记卡片")
        return unique

    def _extract_card_title(self, node: ET.Element) -> str:
        """
        从卡片 ViewGroup 节点中提取笔记标题文字。
        优先取最长的 TextView 子节点文本。
        """
        texts: list[str] = []
        # 遍历所有子孙节点
        for child in node.iter():
            cls = child.get("class", "")
            if "TextView" not in cls:
                continue
            t = (child.get("text") or "").strip()
            if t and len(t) > 2:   # 排除极短文本（如点赞数"12"）
                texts.append(t)
        if not texts:
            return ""
        # 返回最长文本（通常是标题，而非数字或用户名）
        return max(texts, key=len)

    # ================================================================== #
    # 笔记详情：打开 → 互动 → 返回
    # ================================================================== #

    def _process_card(self, card: NoteCard) -> None:
        """
        点开一张笔记卡片，执行互动判断，然后返回 Feed。
        """
        logger.info(f"打开笔记: {card.title[:30]!r} @ ({card.cx},{card.cy})")
        self._stats.notes_seen += 1

        # 点击卡片（随机偏移）
        self.d.click(
            card.cx + random.randint(-10, 10),
            card.cy + random.randint(-8, 8),
        )

        # 等待笔记详情加载
        loaded = self._wait_for_note_detail(timeout=6.0)
        if not loaded:
            logger.warning(f"笔记详情加载超时，返回 Feed")
            self.d.press("back")
            time.sleep(1.0)
            return

        # 模拟阅读停留
        read_time = random.uniform(self.cfg.min_read_seconds, self.cfg.max_read_seconds)
        logger.debug(f"阅读停留 {read_time:.1f}s")
        time.sleep(read_time)

        # 检测是否跑步相关
        is_running = self.action.is_running_note()
        if is_running:
            self._stats.notes_running += 1
            logger.info(f"✓ 跑步相关笔记: {card.title[:30]!r}")
            self._maybe_interact()
        else:
            logger.debug(f"非跑步笔记，跳过互动")

        # 返回 Feed
        self.d.press("back")
        time.sleep(random.uniform(0.8, 1.5))

        # 确保回到首页（有时 back 后在中间层）
        activity = self.nav.current_activity()
        if activity != "IndexActivityV2":
            logger.debug(f"back 后仍在 {activity}，再按一次 back")
            self.d.press("back")
            time.sleep(1.0)

    def _wait_for_note_detail(self, timeout: float = 6.0) -> bool:
        """
        等待笔记详情页加载完成。

        判定：底部操作栏出现（存在含"赞"或"收藏" content-desc 的节点，
        位于屏幕下方 20% 区域）；或 Activity 不再是 IndexActivityV2。

        Returns:
            True 表示详情页已加载。
        """
        screen_h = self.d.info.get("displayHeight", 2400)
        bar_top = int(screen_h * 0.78)
        deadline = time.time() + timeout
        poll_interval = 0.5

        while time.time() < deadline:
            time.sleep(poll_interval)
            # 快速检查 Activity
            activity = self.nav.current_activity()
            if activity not in ("IndexActivityV2", "unknown"):
                logger.debug(f"进入笔记详情 Activity: {activity}")
                return True
            # 检查底部操作栏特征节点
            try:
                xml_str = self.d.dump_hierarchy()
                root = ET.fromstring(xml_str)
                for node in root.iter():
                    cd = (node.get("content-desc") or "").strip()
                    bounds = node.get("bounds", "")
                    if not bounds or not cd:
                        continue
                    if "赞" not in cd and "收藏" not in cd:
                        continue
                    try:
                        parts = bounds.replace("][", ",").strip("[]").split(",")
                        _, top, _, _ = (int(p) for p in parts)
                        if top > bar_top:
                            logger.debug("检测到操作栏，笔记详情已加载")
                            return True
                    except Exception:
                        pass
            except Exception:
                pass

        return False

    def _maybe_interact(self) -> None:
        """
        按配置概率决定是否点赞 / 收藏。
        满足日限额时才执行，并记录到数据库。
        """
        today_likes = self.db.get_today_note_like_count()
        today_collects = self.db.get_today_note_collect_count()

        do_like = (
            random.random() < self.cfg.like_probability
            and today_likes < self.cfg.daily_like_limit
            and self._likes_this_session < self.cfg.daily_like_limit
        )
        do_collect = (
            random.random() < self.cfg.collect_probability
            and today_collects < self.cfg.daily_collect_limit
            and self._collects_this_session < self.cfg.daily_collect_limit
        )

        liked = False
        collected = False

        if do_like:
            if self.dry_run:
                logger.info("[DRY RUN] 跳过点赞")
                liked = True
            else:
                liked = self.action.tap_like()
                if liked:
                    self._likes_this_session += 1
                    self._stats.likes += 1
                    self.db.record_note_like(self.session_id)

        # 收藏前稍作停顿（避免连续操作被检测）
        if do_collect:
            if liked:
                time.sleep(random.uniform(0.5, 1.5))
            if self.dry_run:
                logger.info("[DRY RUN] 跳过收藏")
                collected = True
            else:
                collected = self.action.tap_collect()
                if collected:
                    self._collects_this_session += 1
                    self._stats.collects += 1
                    self.db.record_note_collect(self.session_id)

        if liked or collected:
            self._stats.notes_interacted += 1

    # ================================================================== #
    # Feed 滚动
    # ================================================================== #

    def _scroll_feed(self) -> None:
        """
        在首页瀑布流中向下滚动一页。
        使用 human_swipe_up 模拟真人快速上滑。
        """
        human_swipe_up(self.d)
        logger.debug("Feed 向下滚动一页")

    # ================================================================== #
    # 限额检查
    # ================================================================== #

    def _is_over_daily_limit(self) -> bool:
        """检查今日点赞/收藏是否已达上限（取最保守一侧）。"""
        today_likes = self.db.get_today_note_like_count()
        today_collects = self.db.get_today_note_collect_count()
        over_like = today_likes >= self.cfg.daily_like_limit
        over_collect = today_collects >= self.cfg.daily_collect_limit
        if over_like:
            logger.info(f"今日点赞已达上限 {self.cfg.daily_like_limit}")
        if over_collect:
            logger.info(f"今日收藏已达上限 {self.cfg.daily_collect_limit}")
        return over_like and over_collect
