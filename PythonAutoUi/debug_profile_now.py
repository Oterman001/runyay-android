"""分析当前手机屏幕上的博主主页，截图 + 提取关键信息。"""
import sys
import xml.etree.ElementTree as ET
import uiautomator2 as u2
from utils.cn_number import parse_cn_number
from persistence.database import Database

d = u2.connect()

# 截图
d.screenshot("data/debug_current_profile.png")
print("截图已保存: data/debug_current_profile.png")

# dump 页面结构
xml_str = d.dump_hierarchy()
root = ET.fromstring(xml_str)

print("\n=== 当前页面关键文本 ===")
seen = set()
for node in root.iter():
    t = (node.get("text", "") or "").strip()
    cd = (node.get("content-desc", "") or "").strip()
    cls = node.get("class", "").split(".")[-1]
    bounds = node.get("bounds", "")
    val = t or cd
    if val and len(val) > 1 and len(val) < 80 and val not in seen:
        seen.add(val)
        print(f"  [{cls}] {val!r}  bounds={bounds}")

# 解析关键数据
print("\n=== 解析博主数据 ===")
username = None
followers = 0
following = 0

for node in root.iter():
    cd = (node.get("content-desc", "") or "").strip()
    if cd.endswith("粉丝") and cd[0].isdigit():
        followers = parse_cn_number(cd[: cd.rfind("粉丝")])
        print(f"粉丝数: {followers}  (from cd={cd!r})")
    if cd.endswith("关注") and cd[0].isdigit():
        following = parse_cn_number(cd[: cd.rfind("关注")])
        print(f"关注数: {following}  (from cd={cd!r})")

# 查询数据库里对此博主的记录
print("\n=== 数据库查询（最近5条关注记录）===")
try:
    db = Database("data/xhs_automation.db")
    import sqlite3
    conn = sqlite3.connect("data/xhs_automation.db")
    conn.row_factory = sqlite3.Row
    rows = conn.execute("""
        SELECT fr.blogger_username, fr.followed_at, fr.follow_confirmed,
               b.followers, b.following, b.follow_score, b.profile_note
        FROM follow_records fr
        LEFT JOIN bloggers b ON b.username = fr.blogger_username
        ORDER BY fr.followed_at DESC
        LIMIT 5
    """).fetchall()
    for r in rows:
        print(dict(r))
    conn.close()
    db.close()
except Exception as e:
    print(f"DB 查询失败: {e}")

# 查最新会话日志
print("\n=== 最新日志文件尾部 ===")
import glob, os
from pathlib import Path
logs = sorted(glob.glob("data/logs/run_*.log"))
if logs:
    latest = logs[-1]
    print(f"日志文件: {latest}")
    lines = Path(latest).read_text(encoding="utf-8", errors="replace").splitlines()
    for line in lines[-40:]:
        print(line)
