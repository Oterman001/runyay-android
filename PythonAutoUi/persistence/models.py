"""
数据模型：用于 profile_parser 的输出和数据库记录。
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional


@dataclass
class BloggerProfile:
    """从小红书主页解析出的博主信息。"""
    username: str
    followers: int = 0        # 粉丝数
    following: int = 0        # 关注数
    notes_count: int = 0      # 笔记数
    follow_score: int = 0     # 互关可能性评分 0-100
    is_already_followed: bool = False   # 已关注
    is_mutual_follow: bool = False      # 已互关
    is_verified: bool = False           # 蓝 V 认证
    bio_text: str = ""                    # 简介全文（用于跑步/互关判定）
    social_proof_text: str = ""           # "X等N人关注了他" 文本（关注圈判定）
    profile_note: str = ""              # 跳过原因或备注
    discovered_at: datetime = field(default_factory=datetime.now)


@dataclass
class FollowRecord:
    """关注动作记录。"""
    blogger_username: str
    session_id: int
    followed_at: datetime = field(default_factory=datetime.now)
    follow_confirmed: bool = False   # 关注后验证按钮变化
    followed_back: Optional[bool] = None  # 回关状态（后续核查填充）


@dataclass
class RunSession:
    """单次运行会话摘要。"""
    id: Optional[int] = None
    started_at: datetime = field(default_factory=datetime.now)
    ended_at: Optional[datetime] = None
    keywords_used: str = "[]"         # JSON 数组字符串
    candidates_seen: int = 0
    candidates_qualified: int = 0
    follows_made: int = 0
    stop_reason: str = ""             # daily_limit | session_limit | error | complete
    my_following_end: int = 0         # 会话结束时账号的关注数
    my_followers_end: int = 0         # 会话结束时账号的粉丝数
