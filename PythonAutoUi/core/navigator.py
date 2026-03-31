"""
XHS 屏幕导航：提供命名屏幕切换，检测当前所在页面。
"""

from __future__ import annotations

import re
import random
import subprocess
import time

import uiautomator2 as u2
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_fixed

# XHS Activity 短名 → 屏幕标识（RelationMergeActivity 需进一步区分）
_ACTIVITY_MAP = {
    "IndexActivityV2":       "home",           # 首页 / 我的主页
    "NewOtherUserActivity":  "other_profile",  # 其他博主主页
}


class Navigator:
    def __init__(self, d: u2.Device):
        self.d = d

    # ------------------------------------------------------------------ #
    # 当前屏幕检测
    # ------------------------------------------------------------------ #

    def current_activity(self) -> str:
        """
        通过 adb dumpsys window 获取当前前台 Activity 短名。

        实机探测结果：
            IndexActivityV2          → 首页 / 我的主页
            NewOtherUserActivity     → 其他博主主页
            RelationMergeActivity    → 关注列表（我的 OR 博主的，需 UI 进一步区分）

        返回 Activity 类名最后一段（如 'IndexActivityV2'），失败返回 'unknown'。
        """
        try:
            result = subprocess.run(
                ["adb", "shell", "dumpsys", "window"],
                capture_output=True, text=True, timeout=6,
                encoding="utf-8", errors="replace",
            )
            for line in result.stdout.splitlines():
                if "mCurrentFocus=" in line and "null" not in line:
                    m = re.search(r"com\.xingin\.xhs/[\w.]+\.(\w+)\}", line)
                    if m:
                        return m.group(1)
        except Exception as e:
            logger.debug(f"current_activity 获取失败: {e}")
        return "unknown"

    def current_screen(self) -> str:
        """
        返回当前所在屏幕名称（以 Activity 为主信号，UI 辅助区分子状态）：

            'home'               首页 / 我的主页（IndexActivityV2）
            'other_profile'      其他博主主页（NewOtherUserActivity）
            'my_following'       我的关注列表（RelationMergeActivity + 互相关注 Tab）
            'blogger_following'  博主关注列表（RelationMergeActivity，无互相关注 Tab）
            'unknown'            未识别

        相比纯 UI 检测，Activity 名称更稳定，不受多语言/版本 UI 变化影响。
        """
        activity = self.current_activity()
        logger.debug(f"current_activity={activity}")

        if activity in _ACTIVITY_MAP:
            return _ACTIVITY_MAP[activity]

        if activity == "RelationMergeActivity":
            # 我的关注列表有独有的"互相关注" Tab（4 个 Tab），博主关注列表无此 Tab
            if self.d(text="互相关注").exists:
                return "my_following"
            return "blogger_following"

        return "unknown"

    def wait_for_screen(self, expected: str, timeout: float = 8.0) -> bool:
        """
        等待当前屏幕变为 expected，每 0.8s 轮询一次。

        Args:
            expected: 目标屏幕名（与 current_screen() 返回值一致）
            timeout:  最长等待秒数

        Returns:
            True 表示在超时前到达目标屏幕，False 表示超时。
        """
        deadline = time.time() + timeout
        while time.time() < deadline:
            if self.current_screen() == expected:
                logger.debug(f"已到达目标界面: {expected}")
                return True
            time.sleep(0.8)
        logger.warning(f"等待界面 {expected!r} 超时（{timeout}s）")
        return False

    # ------------------------------------------------------------------ #
    # 导航动作
    # ------------------------------------------------------------------ #

    def go_home(self) -> None:
        """点击底部"首页"Tab。"""
        self.d(text="首页").click()
        time.sleep(1.0)
        logger.debug("已回到首页")

    @retry(stop=stop_after_attempt(3), wait=wait_fixed(1))
    def go_to_search(self) -> None:
        """从首页进入搜索输入框。"""
        d = self.d
        # 搜索图标可能是 content-desc 或 text
        if d(contentDescription="搜索").exists:
            d(contentDescription="搜索").click()
        elif d(text="搜索").exists:
            d(text="搜索").click()
        else:
            # 右上角放大镜 icon（fallback）
            d.xpath('//*[@content-desc="搜索" or @text="搜索"]').click()

        # 等待搜索输入框获得焦点
        if not d(focused=True, className="android.widget.EditText").wait(timeout=5):
            raise RuntimeError("搜索输入框未出现")
        logger.debug("搜索输入框已就绪")

    def go_back(self) -> None:
        """返回上一页。"""
        self.d.press("back")
        time.sleep(0.8)

    def go_back_to_search_results(self) -> None:
        """从用户主页返回搜索结果列表。"""
        self.go_back()
        # 等待搜索结果标签出现
        self.d(text="用户").wait(timeout=5)
        logger.debug("已返回搜索结果")

    # ------------------------------------------------------------------ #
    # 推荐列表路径导航（v2）
    # ------------------------------------------------------------------ #

    def go_to_my_tab(self) -> None:
        """
        点击底部'我' Tab，进入我的主页。
        实机验证：XHS 底部 Tab 全部使用 text 属性，contentDescription 为空。
        """
        d = self.d
        # text="我" 是底部最右侧的 Tab
        found = d(text="我").wait(timeout=8)
        if not found:
            raise RuntimeError("底部'我' Tab 未找到")
        d(text="我").click()
        time.sleep(2.0)
        # 等待我的主页加载：「编辑资料」Button 唯一出现在我的主页，不在关注列表/首页
        # 不使用 text='粉丝'/'关注'，这两个 Tab 在关注列表顶部也存在，会立即误判
        found = (
            d(text="编辑资料", className="android.widget.Button").wait(timeout=12)
            or d(textContains="编辑").wait(timeout=3)  # 兼容部分版本文字差异
        )
        if not found:
            # 有时加载慢，press_back 清理后再试一次
            logger.debug("'编辑资料'未出现，清理后重试...")
            d.press("back")
            time.sleep(1.5)
            d(text="我").wait(timeout=5)
            d(text="我").click()
            time.sleep(3.0)
            found = (
                d(text="编辑资料", className="android.widget.Button").wait(timeout=12)
                or d(textContains="编辑").wait(timeout=3)
            )
        if not found:
            raise RuntimeError("我的主页未加载（未找到'编辑资料'按钮）")
        logger.debug("已进入'我的'主页")

    def click_following_count(self) -> None:
        """
        在我的主页，点击'关注'数字 Button 进入关注列表。
        实机验证：统计区是一行 3 个空文本 Button，从左到右依次是：
          Button[0] = 关注数（x≈49-151），Button[1] = 粉丝数，Button[2] = 获赞收藏
        直接点击最左侧的 Button 即可进入关注列表。
        """
        d = self.d
        import xml.etree.ElementTree as ET

        # 获取统计行中最左侧的 Button（即关注数按钮）
        clicked = False
        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            stat_buttons = []
            for node in root.iter():
                cls = node.get("class", "")
                b = node.get("bounds", "")
                t = node.get("text", "").strip()
                if "Button" in cls and not t and b:
                    try:
                        parts = b.replace("][", ",").strip("[]").split(",")
                        l, top, r, bot = int(parts[0]), int(parts[1]), int(parts[2]), int(parts[3])
                        h = bot - top
                        # 统计 Button 高度约 100px，宽约 100-200px，排除全屏 Button
                        if 60 < h < 160 and (r - l) < 400:
                            stat_buttons.append((l, top, r, bot))
                    except Exception:
                        pass
            if stat_buttons:
                # 只取 y_top 在 600-1100 范围内的（统计区，排除其他行按钮）
                stat_buttons = [(l, top, r, bot) for l, top, r, bot in stat_buttons if 600 < top < 1100]
                # 按 x 坐标排序，取最左侧（关注数按钮）
                stat_buttons.sort(key=lambda x: x[0])
                l, top, r, bot = stat_buttons[0]
                cx = (l + r) // 2
                cy = (top + bot) // 2
                logger.debug(f"点击关注数按钮: ({cx},{cy})")
                d.click(cx, cy)
                clicked = True
        except Exception as e:
            logger.debug(f"统计按钮定位失败: {e}")

        if not clicked:
            # fallback：直接点击 text='关注' 标签区域（在统计行左侧）
            if d(text="关注").exists:
                d(text="关注").click()
            else:
                d.click(100, 880)  # 固定坐标兜底

        time.sleep(1.8)
        # 等待关注列表页加载：'推荐' Tab 出现（text 属性）
        if not d(text="推荐").wait(timeout=8):
            raise RuntimeError("底部'我' Tab 未找到")
        logger.debug("已进入关注列表页")

    @retry(stop=stop_after_attempt(3), wait=wait_fixed(1))
    def switch_to_recommend_tab(self) -> None:
        """
        在关注列表页，点击顶部'推荐' Tab。
        实机验证：Tab 使用 text 属性，不是 description。
        """
        d = self.d
        if not d(text="推荐").wait(timeout=5):
            raise RuntimeError("未找到'推荐' Tab")
        d(text="推荐").click()
        time.sleep(2.0)
        logger.debug("已切换到推荐列表")

    def switch_to_following_tab(self) -> None:
        """
        在关注列表页，点击顶部'关注' Tab（我已关注的人）。

        注意：页面上同时有 text='关注' 的 Tab 控件（上方，y < 400）
        和列表条目右侧的「已关注」按钮，需要靠 y 坐标区分。
        """
        import xml.etree.ElementTree as ET

        d = self.d
        clicked = False
        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            for node in root.iter():
                t = node.get("text", "").strip()
                cls = node.get("class", "")
                bounds = node.get("bounds", "")
                if t == "关注" and "TextView" in cls and bounds:
                    try:
                        parts = bounds.replace("][", ",").strip("[]").split(",")
                        l, top, r, bot = int(parts[0]), int(parts[1]), int(parts[2]), int(parts[3])
                        # Tab 控件在屏幕上方（y < 400），排除列表中的"已关注"按钮
                        if top < 400:
                            cx, cy = (l + r) // 2, (top + bot) // 2
                            logger.debug(f"点击'关注' Tab: ({cx},{cy})")
                            d.click(cx, cy)
                            clicked = True
                            break
                    except Exception:
                        pass
        except Exception as e:
            logger.debug(f"切换'关注' Tab 解析失败: {e}")

        if not clicked:
            # Fallback：直接用 text="关注" 点击 Tab
            if d(text="关注").exists:
                d(text="关注").click()

        time.sleep(1.5)
        logger.debug("已切换到'关注' Tab（我的关注列表）")

    def go_back_to_recommend_list(self, depth: int = 1) -> None:
        """
        从用户主页（可能有多层深入）返回推荐列表。
        depth: 需要按返回键的次数。
        """
        for _ in range(depth):
            self.d.press("back")
            time.sleep(0.8)
        # 确认推荐 Tab 可见（在关注列表页）
        self.d(description="推荐").wait(timeout=5)
        logger.debug(f"已返回推荐列表（back x{depth}）")

    # ------------------------------------------------------------------ #
    # 读取「我的」统计数据
    # ------------------------------------------------------------------ #

    def pull_to_refresh(self, times: int = 2) -> None:
        """
        在当前页面执行下拉刷新，模拟手指从上方向下拉动。

        参数：
            times: 刷新次数（默认 2 次，确保数据更新）

        手势参数：从 y≈350 拉到 y≈1000，速度较慢以触发系统下拉刷新动画。
        每次刷新后等待 1.5-2.5 秒让数据加载完成。
        """
        d = self.d
        for i in range(times):
            start_y = random.randint(300, 400)
            end_y   = random.randint(900, 1100)
            dur     = random.uniform(0.6, 1.0)
            d.swipe(540, start_y, 540, end_y, duration=dur)
            wait = random.uniform(1.5, 2.5)
            logger.debug(f"下拉刷新第 {i + 1}/{times} 次，等待 {wait:.1f}s")
            time.sleep(wait)

    def read_my_stats(self, pull_refresh: bool = False) -> dict:
        """
        导航到「我的」Tab，可选下拉刷新后读取关注数与粉丝数。

        参数：
            pull_refresh: True 时在读取前执行 2-3 次下拉刷新，
                          确保关注操作后的数据已更新（无需重启 App）。
        返回格式：{"following": int, "followers": int}
        """
        import re
        import xml.etree.ElementTree as ET
        from utils.cn_number import parse_cn_number

        d = self.d
        result = {"following": 0, "followers": 0}

        # 进入我的主页
        self.go_to_my_tab()

        # 可选：下拉刷新若干次让关注数/粉丝数更新
        if pull_refresh:
            refresh_times = random.randint(2, 3)
            logger.info(f"下拉刷新 {refresh_times} 次，等待数据更新...")
            self.pull_to_refresh(times=refresh_times)

        time.sleep(1.0)

        # dump 页面 XML，用兄弟节点法提取数值
        xml_str = d.dump_hierarchy()
        root = ET.fromstring(xml_str)

        label_map = {"关注": "following", "粉丝": "followers"}
        for label, key in label_map.items():
            for parent in root.iter():
                children = list(parent)
                for i, child in enumerate(children):
                    if (
                        child.get("text", "").strip() == label
                        and "TextView" in child.get("class", "")
                    ):
                        # 向前找第一个含数字的兄弟节点
                        for j in range(i - 1, -1, -1):
                            num_text = children[j].get("text", "").strip()
                            if num_text and re.search(r"\d", num_text):
                                result[key] = parse_cn_number(num_text)
                                break
                        break
                if result[key]:
                    break

        logger.info(
            f"我的账号统计 — 关注: {result['following']}  粉丝: {result['followers']}"
        )
        return result

    def click_blogger_following_count(self) -> bool:
        """
        在博主主页，点击「关注数」统计按钮，尝试进入其关注列表。

        小红书博主主页统计区按钮 content-desc 格式为 "8387关注"（数字+关注），
        与关注操作按钮（text="关注"）区分方式：
          - 统计按钮：contentDescription 以"关注"结尾且不以"关注"/"+"开头
          - 关注操作按钮：text="关注"，无 contentDescription 或 contentDescription 不含数字

        进入成功判定：点击后页面不再含有博主主页专有的 follow 操作按钮
        （"关注"/"已关注"/"互相关注" Button），同时能收集到列表用户名。
        不成功时（私密/受限）静默返回 False，不抛异常。
        """
        import xml.etree.ElementTree as ET

        d = self.d
        clicked = False

        # ── 方案1：content-desc 以"关注"结尾的统计按钮（如 "8387关注"） ────
        try:
            xml_str = d.dump_hierarchy()
            root = ET.fromstring(xml_str)
            for node in root.iter():
                cls = node.get("class", "")
                if "Button" not in cls:
                    continue
                cd = (node.get("content-desc", "") or "").strip()
                if not cd:
                    cd = (node.get("contentDescription", "") or "").strip()
                # 以"关注"结尾，且首字符为数字（排除 "+关注" 之类操作按钮）
                if cd.endswith("关注") and cd[0].isdigit():
                    bounds = node.get("bounds", "")
                    if bounds:
                        try:
                            parts = bounds.replace("][", ",").strip("[]").split(",")
                            l, top, r, bot = (int(p) for p in parts)
                            cx, cy = (l + r) // 2, (top + bot) // 2
                            logger.debug(f"点击博主关注数按钮: cd={cd!r} ({cx},{cy})")
                            d.click(cx, cy)
                            clicked = True
                        except Exception:
                            node.click() if hasattr(node, "click") else d.click(cx, cy)
                            clicked = True
                    break
        except Exception as e:
            logger.debug(f"博主关注数按钮(cd方案)查找失败: {e}")

        if not clicked:
            # ── 方案2：找 text='关注' 标签，点击其父统计 Button（XML 兄弟节点） ──
            try:
                xml_str = d.dump_hierarchy()
                root = ET.fromstring(xml_str)
                for parent in root.iter():
                    children = list(parent)
                    for i, child in enumerate(children):
                        t = child.get("text", "").strip()
                        cls = child.get("class", "")
                        if t == "关注" and "TextView" in cls:
                            # 找前一个兄弟（数字节点），再找父 Button
                            pb = parent.get("bounds", "")
                            pc = parent.get("class", "")
                            if "Button" in pc and pb:
                                try:
                                    parts = pb.replace("][", ",").strip("[]").split(",")
                                    l, top, r, bot = (int(p) for p in parts)
                                    cx, cy = (l + r) // 2, (top + bot) // 2
                                    logger.debug(f"点击博主关注数按钮(兄弟节点方案): ({cx},{cy})")
                                    d.click(cx, cy)
                                    clicked = True
                                except Exception:
                                    pass
                            break
                    if clicked:
                        break
            except Exception as e:
                logger.debug(f"博主关注数按钮(xml方案)查找失败: {e}")

        if not clicked:
            logger.debug("未找到博主关注数按钮，跳过关注列表探索")
            return False

        time.sleep(2.0)

        # ── 验证是否成功进入博主关注列表（Activity 为主信号） ─────────────
        screen = self.current_screen()
        logger.debug(f"click_blogger_following_count 后: current_screen={screen}")
        if screen == "blogger_following":
            logger.debug("已进入博主关注列表（Activity 确认）")
            return True
        if screen == "other_profile":
            logger.debug("仍在博主主页（Activity 确认），未能进入关注列表")
            return False
        # Activity 未变化或未识别，回退到 UI 宽度判断
        try:
            for follow_text in ("关注", "已关注", "互相关注"):
                for btn in d(text=follow_text, className="android.widget.Button"):
                    info = btn.info
                    bounds = info.get("bounds", {})
                    if bounds:
                        w = bounds.get("right", 0) - bounds.get("left", 0)
                        if 150 < w < 350:
                            logger.debug(f"仍在博主主页（{follow_text!r} 按钮可见），未能进入关注列表")
                            return False
        except Exception:
            pass

        logger.debug("已进入博主关注列表（UI 兜底确认）")
        return True
