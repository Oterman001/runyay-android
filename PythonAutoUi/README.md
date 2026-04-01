# 小红书跑步博主自动关注脚本 — 使用说明书

> 基于 uiautomator2 驱动 Android 设备，模拟真实用户操作行为，自动发现并关注小红书跑步博主。

---

## 目录

- [项目结构](#项目结构)
- [环境准备](#环境准备)
- [设备连接](#设备连接)
- [配置说明](#配置说明)
- [运行脚本](#运行脚本)
- [清理消息脚本](#清理消息脚本)
- [运行模式详解](#运行模式详解)
- [评分算法](#评分算法)
- [数据库说明](#数据库说明)
- [防检测机制](#防检测机制)
- [常见问题](#常见问题)

---

## 项目结构

```
PythonAutoUi/
├── main.py                    # 主入口：自动关注脚本
├── clear_messages.py          # 辅助工具：清理未读消息
├── config.yaml                # 配置文件（修改此处调整行为）
├── data/
│   └── xhs_automation.db      # SQLite 数据库（自动创建）
│
├── core/
│   ├── device.py              # 设备连接 & 锁屏/解锁管理
│   └── navigator.py           # UI 导航（Tab 切换、返回、读取统计）
│
├── xhs/
│   ├── recommender.py         # 推荐列表 & 我的关注列表核心流程
│   ├── profile_parser.py      # 博主主页数据解析
│   ├── follow_action.py       # 关注按钮点击 & 限流检测
│   ├── note_explorer.py       # 笔记流浏览 & 点赞/收藏
│   └── note_action.py         # 笔记详情页互动操作
│
├── strategy/
│   ├── scoring.py             # 互关可能性评分算法
│   └── daily_limit.py         # 每日关注限额检查
│
├── persistence/
│   ├── database.py            # SQLite CRUD 操作
│   └── models.py              # 数据模型（BloggerProfile / RunSession）
│
├── anti_detection/
│   ├── human_delay.py         # 拟人化随机延迟（高斯分布）
│   └── gesture.py             # 仿真滑动手势
│
└── utils/
    ├── config_loader.py       # YAML 配置加载 & Pydantic 校验
    ├── logger.py              # Loguru 日志初始化
    ├── process_guard.py       # 进程互斥守卫（防重复启动）
    └── cn_number.py           # 中文数字解析（"1.2万" → 12000）
```

---

## 环境准备

### Python 版本

Python **3.10+**（推荐 3.11）

### 安装依赖

```bash
cd PythonAutoUi
pip install -r requirements.txt
# 或使用 .venv 虚拟环境
.venv\Scripts\activate
```

主要依赖包：

| 包名 | 用途 |
|------|------|
| `uiautomator2` | Android UI 自动化框架 |
| `loguru` | 结构化日志 |
| `rich` | 终端彩色输出 & 表格 |
| `pydantic` | 配置校验 |
| `pyyaml` | 配置文件解析 |
| `tenacity` | 自动重试 |
| `psutil` | 进程管理 |

---

## 设备连接

### USB 连接

1. 手机开启**开发者模式** → 启用 **USB 调试**
2. 连接数据线后执行：

```bash
adb devices
```

输出示例：
```
List of devices attached
192.168.1.100:5555    device
```

3. 将序列号填入 `config.yaml` 的 `device.serial`，或通过 `--serial` 参数传入。

### WiFi 连接

```bash
# 先用 USB 连上后执行
adb tcpip 5555
adb connect 192.168.1.100:5555
```

然后在配置文件中设置：
```yaml
device:
  serial: "192.168.1.100:5555"
```

### 首次连接说明

首次连接时 uiautomator2 会自动安装 **ATX Agent** 到手机，需要约 30 秒，之后无需重复安装。

---

## 配置说明

编辑 `config.yaml` 调整所有行为参数：

### 设备配置

```yaml
device:
  serial: ""          # 留空自动检测唯一连接的设备
```

### 运行控制

```yaml
dry_run: false        # true = 只打日志不实际关注（测试用）

daily_follow_limit: 97          # 每天最多关注人数（建议 30-45）
max_follows_per_session: 100    # 单次会话上限
session_break_seconds: 30       # 达到单次上限后的休息时长（秒）
active_hours: [6, 24]           # 仅在此时间段内运行（24小时制）
```

> **安全建议**：`daily_follow_limit` 建议设置在 **30-45** 之间，避免触发平台限流。

### 运行模式

```yaml
# auto（随机整合两种路径，默认）
# recommend（仅推荐列表路径）
# my_following（仅我的关注列表探索路径）
# explore_notes（笔记流点赞/收藏，独立于关注功能）
mode: "my_following"
```

### 推荐列表参数

```yaml
pages_per_recommend: 200   # 推荐列表滚动页数（每页约 6-8 个用户）
max_depth: 5               # 横向推荐最大深入层数（1=不深入）
direct_follow: false       # true=直接点关注按钮（快速，无评分过滤）
                           # false=进主页评分后再关注（默认，更精准）
```

### 互关评分过滤

```yaml
min_score_threshold: 60    # 评分阈值 0-100，低于此分值跳过
```

### 我的关注列表探索模式专用

```yaml
max_bloggers_to_explore: 500    # 从我的关注列表最多取多少博主作为入口
restart_after_follows: 50       # 累计关注超过此数则重启应用（0=不重启）
random_skip_ratio: 0.12         # 随机跳过博主的概率（模拟真实浏览）
```

### 笔记探索配置

```yaml
note_explore:
  tabs: ["关注", "发现"]         # 浏览的 Tab 顺序
  pages_per_tab: 5               # 每个 Tab 滚动页数
  daily_like_limit: 50           # 每日点赞上限
  daily_collect_limit: 30        # 每日收藏上限
  like_probability: 0.70         # 命中跑步笔记后点赞概率
  collect_probability: 0.45      # 命中跑步笔记后收藏概率
  min_read_seconds: 3.0          # 最短"阅读"停留时间（秒）
  max_read_seconds: 9.0          # 最长"阅读"停留时间（秒）
```

### 延迟配置（高斯分布）

```yaml
delays:
  tap:
    mu: 0.8          # 点击前停顿均值（秒）
    sigma: 0.3
  between_profiles:
    mu: 3.0          # 浏览博主间隔均值
    sigma: 1.2
  post_follow:
    mu: 10.0         # 关注后等待均值（最关键）
    sigma: 4.0
  long_break_probability: 0.15   # 关注后触发长暂停概率（15%）
  long_break_extra_min: 20       # 长暂停额外时间下限（秒）
  long_break_extra_max: 60       # 长暂停额外时间上限（秒）
  scroll_pause:
    mu: 1.5          # 翻页停顿均值
    sigma: 0.6
```

---

## 运行脚本

### 基本用法

```bash
# 默认模式（auto，随机整合两种路径）
python main.py

# 显式指定模式
python main.py --mode auto
python main.py --mode recommend
python main.py --mode my_following

# 笔记探索模式：浏览首页瀑布流，对跑步笔记点赞/收藏
python main.py --mode explore_notes

# 模拟运行（不实际操作，仅打印日志）
python main.py --dry-run
python main.py --mode explore_notes --dry-run

# 覆盖每日关注限额（本次运行临时生效）
python main.py --limit 20

# 指定设备序列号（多设备场景）
python main.py --serial 192.168.1.100:5555
python main.py --mode explore_notes --serial 43d9cd28

# 组合参数
python main.py --mode recommend --limit 30 --serial 192.168.1.100:5555
```

### 命令行参数一览

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `--mode` | `auto\|recommend\|my_following\|explore_notes` | 读取 config.yaml | 运行模式 |
| `--dry-run` | flag | false | 模拟运行，不实际执行操作 |
| `--limit` | int | 0（使用配置值） | 覆盖每日关注限额 |
| `--serial` | string | 空（使用配置值） | 设备序列号，多设备时指定 |

### 运行输出示例

```
小红书跑步博主自动关注 — 正式运行
模式: my_following | 每日上限: 97 | 评分阈值: 60 | 最大深度: 5

┌─────────────────────────────────────────┐
│              会话总结                    │
├──────────────┬──────────────────────────┤
│ 指标          │ 值                       │
├──────────────┼──────────────────────────┤
│ 扫描候选      │ 128                      │
│ 符合条件      │ 43                       │
│ 成功关注      │ 28                       │
│ 今日累计      │ 28                       │
│ 停止原因      │ complete                 │
└──────────────┴──────────────────────────┘
```

---

## 清理消息脚本

用于清理小红书「消息」Tab 中的未读通知，避免消息积压干扰自动关注流程。

```bash
# 清理所有未读消息
python clear_messages.py

# 最多清理 10 条
python clear_messages.py --limit 10

# 指定设备
python clear_messages.py --serial 192.168.1.100:5555
```

| 参数 | 说明 |
|------|------|
| `--limit N` | 最多清理 N 条（0 = 不限，全部清理） |
| `--serial` | 设备序列号 |

---

## 运行模式详解

### `recommend` — 推荐列表模式

**导航路径**：「我的」Tab → 关注数量按钮 → 推荐 Tab

- 在推荐列表中依次扫描用户
- 若 `direct_follow: false`：进入每个博主主页 → 解析数据 → 评分 → 关注后处理横向推荐
- 若 `direct_follow: true`：直接点击列表中的"关注"按钮（速度快，无评分过滤）
- 滚动页数由 `pages_per_recommend` 控制

### `my_following` — 我的关注列表探索模式

**导航路径**：「我的」Tab → 关注数量按钮 → 关注 Tab → 进入各博主关注列表

- 从已关注的博主中选取入口（最多 `max_bloggers_to_explore` 个）
- 进入每个博主的关注列表，扫描其关注的人
- 对符合条件的用户进行评分并关注
- 累计关注达到 `restart_after_follows` 后自动重启应用

### `auto` — 自动整合模式（默认）

- 将 `recommend` 和 `my_following` 两种路径**随机排序**后依次执行
- 两段路径切换之间插入 **15-45 秒**随机停顿
- 每段路径开始前检查每日限额，达到上限则提前结束

### `explore_notes` — 笔记探索模式

**导航路径**：首页 → 关注 Tab / 发现 Tab → 瀑布流卡片 → 笔记详情

- 遍历 `note_explore.tabs` 配置的首页 Tab（默认：关注、发现）
- 每个 Tab 下滚动 `pages_per_tab` 页，每页随机选取 1-3 张**图文笔记**（自动过滤视频）
- 进入笔记详情后模拟真实阅读停留（`min_read_seconds` ~ `max_read_seconds`）
- 检测笔记内容是否包含跑步相关关键词（跑步、马拉松、配速、Garmin 等 60+ 词）
- 命中后按概率决定是否点赞（`like_probability`）和收藏（`collect_probability`）
- 每日点赞/收藏次数受 `daily_like_limit` / `daily_collect_limit` 硬性限制，达到上限自动停止
- 操作结果写入 `note_interactions` 表，跨会话累计统计

**会话输出示例：**
```
       笔记探索总结
+-------------------------+
| 指标         |       值 |
|--------------+----------|
| 浏览笔记     |        8 |
| 跑步笔记     |        7 |
| 点赞         |        5 |
| 收藏         |        4 |
| 今日点赞累计 |        5 |
| 今日收藏累计 |        4 |
| 停止原因     | complete |
+-------------------------+
```

> **UI 兼容说明**：已在 1080×2373 和 1116×2484 两种分辨率设备上验证，通过 content-desc 精准定位按钮（`点赞 N` / `收藏 N`），无需适配不同分辨率。

---

## 评分算法

评分满分 **100 分**，低于 `min_score_threshold`（默认 60）的博主会被跳过。

### 硬过滤（直接跳过，不计分）

| 条件 | 说明 |
|------|------|
| 已关注 / 已互关 | 无需重复处理 |
| 蓝 V 认证账号 | 大号不太可能回关 |
| 粉丝数 > 200,000 | 大 V，回关概率极低 |
| 关注数 < 50 | 不太主动建立社交关系 |
| 笔记数 = 0 | 无内容账号 |

### 快速通道（直接给 80 分）

昵称包含互关关键词（如"互关"、"有关必回"、"互fo"等）且粉丝数与关注数在同一数量级（相差不超过 10 倍）。

### 四维评分明细

| 维度 | 满分 | 最优区间 |
|------|------|----------|
| 关注/粉丝比（越接近 1:1 越好） | 40 | 比值 0.7~1.5 → 40 分 |
| 账号规模（中小号最可能回关） | 20 | 粉丝 500~5000 → 20 分 |
| 内容活跃度（笔记数适中） | 20 | 笔记 10~200 → 20 分 |
| 关注数绝对值（主动建网络） | 20 | 关注 300~3000 → 20 分 |

---

## 数据库说明

数据自动保存到 `data/xhs_automation.db`（SQLite 格式），包含以下表：

### `bloggers` — 候选博主信息

| 字段 | 说明 |
|------|------|
| `username` | 用户名（唯一） |
| `followers` | 粉丝数 |
| `following` | 关注数 |
| `notes_count` | 笔记数 |
| `follow_score` | 互关评分 |
| `is_verified` | 是否蓝 V |
| `discovered_at` | 发现时间 |

### `follow_records` — 关注记录

| 字段 | 说明 |
|------|------|
| `blogger_username` | 被关注用户名 |
| `session_id` | 所属会话 ID |
| `followed_at` | 关注时间 |
| `follow_confirmed` | 关注动作是否确认成功 |
| `followed_back` | 是否回关（需手动更新） |

### `run_sessions` — 会话摘要

| 字段 | 说明 |
|------|------|
| `started_at` / `ended_at` | 会话时间范围 |
| `candidates_seen` | 扫描的候选人数 |
| `candidates_qualified` | 通过评分的人数 |
| `follows_made` | 实际关注人数 |
| `stop_reason` | 结束原因（`complete` / `rate_limit` / `daily_limit` / `interrupted` / `error`） |
| `my_following_end` | 会话结束时我的关注数 |
| `my_followers_end` | 会话结束时我的粉丝数 |

### `note_interactions` — 笔记互动记录

记录每次点赞（`like`）和收藏（`collect`）操作，用于每日限额控制。

### 查询示例

```sql
-- 今日关注数
SELECT COUNT(*) FROM follow_records WHERE DATE(followed_at) = DATE('now');

-- 各会话汇总
SELECT id, started_at, follows_made, stop_reason FROM run_sessions ORDER BY id DESC LIMIT 10;

-- 近 7 天回关率（手动更新 followed_back 字段后有效）
SELECT
  COUNT(*) AS total,
  SUM(CASE WHEN followed_back = 1 THEN 1 ELSE 0 END) AS backed,
  ROUND(100.0 * SUM(followed_back) / COUNT(*), 1) || '%' AS rate
FROM follow_records
WHERE DATE(followed_at) >= DATE('now', '-7 days');
```

---

## 防检测机制

脚本内置多层拟人化策略，降低被平台识别为自动化脚本的风险：

| 机制 | 实现 |
|------|------|
| **随机延迟** | 所有等待时间使用高斯分布，非固定值 |
| **关注后长停顿** | 15% 概率触发额外 20-60 秒深度停顿 |
| **随机跳过** | `random_skip_ratio` 概率主动跳过部分博主 |
| **仿真滑动** | 滑动轨迹加入贝塞尔曲线和随机抖动 |
| **随机点击偏移** | 点击坐标随机偏移 ±8px |
| **路径随机化** | `auto` 模式下两条路径随机排序 |
| **路径切换停顿** | 路径切换时随机等待 15-45 秒 |
| **定期重启应用** | 累计关注 `restart_after_follows` 次后冷重启 |
| **活跃时段限制** | 只在 `active_hours` 指定时间段内运行 |
| **进程互斥守卫** | 同一时刻只允许一个实例运行，防止冲突 |

---

## 常见问题

### Q：首次运行卡在"正在连接设备"？

ATX Agent 首次安装需要约 30 秒，耐心等待即可。若超时，检查 USB 调试是否开启，或尝试重新插拔数据线。

### Q：报错"等待首页超时，未检测到底部导航栏"？

小红书启动慢或网络加载慢导致。可尝试：
1. 手动打开小红书确认能正常启动
2. 检查手机是否需要登录

### Q：触发平台限流 `RateLimitWarning`？

脚本检测到关注按钮消失或出现限制提示时会立即停止当次会话。建议：
- 降低 `daily_follow_limit`（建议 30-40）
- 提高 `post_follow.mu`（关注后等待时间）

### Q：想查看历史运行记录？

用任意 SQLite 客户端（如 [DB Browser for SQLite](https://sqlitebrowser.org/)）打开 `data/xhs_automation.db` 查看。

### Q：如何只测试不实际关注？

```bash
python main.py --dry-run
```

`dry_run` 模式下所有导航、解析、评分都正常执行，仅跳过实际点击关注按钮的步骤。

### Q：运行后屏幕会自动锁定吗？

会。脚本正常退出后会自动按电源键锁屏（`device.lock_screen()`）。

### Q：两个脚本同时运行会冲突吗？

不会。`process_guard` 模块会在启动时检查是否有旧进程，有则自动终止旧进程再继续。

### Q：`explore_notes` 模式在新手机上需要额外配置吗？

不需要。点赞/收藏按钮通过 content-desc（`点赞 N` / `收藏 N`）定位，与屏幕分辨率和系统版本无关。已在 OPPO、vivo 等多款 Android 设备上验证。

### Q：`explore_notes` 和关注模式能同时用吗？

建议分开运行，避免单次会话操作量过大引发限流。可以先跑关注，再跑笔记探索，中间间隔 30 分钟以上。

### Q：笔记探索时为什么会跳过某些笔记？

以下情况会自动跳过：
- 视频类笔记（UI 布局与图文不同）
- 本次会话已处理过的笔记（标题去重）
- 今日点赞/收藏已达到 `daily_like_limit` / `daily_collect_limit` 上限

---

## 日志文件

日志由 `utils/logger.py` 初始化，输出到控制台（彩色）和 `log/` 目录下的文件（按日期滚动）。

---

*最后更新：2026-04-01 | 新增笔记探索（点赞/收藏）功能*
