"""
SQLite 数据库操作层。

表结构：
  bloggers       — 发现的候选博主
  follow_records — 关注记录
  run_sessions   — 会话摘要
"""

from __future__ import annotations

import json
import sqlite3
from datetime import date, datetime
from pathlib import Path
from typing import Optional

from loguru import logger

from persistence.models import BloggerProfile, FollowRecord, RunSession

_DDL = """
CREATE TABLE IF NOT EXISTS explored_following_sources (
    blogger_username TEXT PRIMARY KEY NOT NULL,
    explored_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bloggers (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT UNIQUE NOT NULL,
    followers       INTEGER DEFAULT 0,
    following       INTEGER DEFAULT 0,
    notes_count     INTEGER DEFAULT 0,
    follow_score    INTEGER DEFAULT 0,
    is_verified     BOOLEAN DEFAULT 0,
    profile_note    TEXT DEFAULT '',
    discovered_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS follow_records (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    blogger_username    TEXT NOT NULL,
    session_id          INTEGER,
    followed_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    follow_confirmed    BOOLEAN DEFAULT 0,
    followed_back       BOOLEAN,
    FOREIGN KEY (session_id) REFERENCES run_sessions(id)
);

CREATE TABLE IF NOT EXISTS run_sessions (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    started_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at                TIMESTAMP,
    keywords_used           TEXT DEFAULT '[]',
    candidates_seen         INTEGER DEFAULT 0,
    candidates_qualified    INTEGER DEFAULT 0,
    follows_made            INTEGER DEFAULT 0,
    stop_reason             TEXT DEFAULT '',
    my_following_end        INTEGER DEFAULT 0,
    my_followers_end        INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_bloggers_username ON bloggers(username);
CREATE INDEX IF NOT EXISTS idx_follow_records_date ON follow_records(followed_at);
"""


class Database:
    def __init__(self, db_path: str = "data/xhs_automation.db"):
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self._conn = sqlite3.connect(db_path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        self._conn.executescript(_DDL)
        self._conn.commit()
        self._migrate()
        logger.debug(f"数据库已连接: {db_path}")

    def _migrate(self) -> None:
        """对旧数据库做向前兼容迁移（新增列）。"""
        migrations = [
            "ALTER TABLE run_sessions ADD COLUMN my_following_end INTEGER DEFAULT 0",
            "ALTER TABLE run_sessions ADD COLUMN my_followers_end INTEGER DEFAULT 0",
        ]
        for sql in migrations:
            try:
                self._conn.execute(sql)
                self._conn.commit()
            except Exception:
                pass  # 列已存在时忽略

    # ------------------------------------------------------------------ #
    # Blogger 操作
    # ------------------------------------------------------------------ #

    def is_already_processed(self, username: str) -> bool:
        """是否已在数据库中（无论是否关注）。"""
        row = self._conn.execute(
            "SELECT 1 FROM bloggers WHERE username = ?", (username,)
        ).fetchone()
        return row is not None

    def save_blogger(self, profile: BloggerProfile) -> None:
        """保存或更新博主信息（upsert）。"""
        self._conn.execute(
            """
            INSERT INTO bloggers (username, followers, following, notes_count,
                                  follow_score, is_verified, profile_note, discovered_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(username) DO UPDATE SET
                followers    = excluded.followers,
                following    = excluded.following,
                notes_count  = excluded.notes_count,
                follow_score = excluded.follow_score,
                is_verified  = excluded.is_verified,
                profile_note = excluded.profile_note
            """,
            (
                profile.username,
                profile.followers,
                profile.following,
                profile.notes_count,
                profile.follow_score,
                int(profile.is_verified),
                profile.profile_note,
                profile.discovered_at.isoformat(),
            ),
        )
        self._conn.commit()

    # ------------------------------------------------------------------ #
    # FollowRecord 操作
    # ------------------------------------------------------------------ #

    def record_follow(self, username: str, session_id: int, confirmed: bool = True) -> None:
        self._conn.execute(
            """
            INSERT INTO follow_records (blogger_username, session_id, follow_confirmed)
            VALUES (?, ?, ?)
            """,
            (username, session_id, int(confirmed)),
        )
        self._conn.commit()
        logger.info(f"[DB] 关注记录已保存: {username}")

    def get_today_follow_count(self) -> int:
        today = date.today().isoformat()
        row = self._conn.execute(
            "SELECT COUNT(*) FROM follow_records WHERE DATE(followed_at) = ?",
            (today,),
        ).fetchone()
        return row[0] if row else 0

    # ------------------------------------------------------------------ #
    # RunSession 操作
    # ------------------------------------------------------------------ #

    def create_session(self, keywords: list[str]) -> RunSession:
        cursor = self._conn.execute(
            "INSERT INTO run_sessions (keywords_used) VALUES (?)",
            (json.dumps(keywords, ensure_ascii=False),),
        )
        self._conn.commit()
        session = RunSession(id=cursor.lastrowid, keywords_used=json.dumps(keywords, ensure_ascii=False))
        logger.info(f"[DB] 新会话创建: id={session.id}")
        return session

    def close_session(self, session: RunSession) -> None:
        session.ended_at = datetime.now()
        self._conn.execute(
            """
            UPDATE run_sessions
            SET ended_at=?, candidates_seen=?, candidates_qualified=?,
                follows_made=?, stop_reason=?
            WHERE id=?
            """,
            (
                session.ended_at.isoformat(),
                session.candidates_seen,
                session.candidates_qualified,
                session.follows_made,
                session.stop_reason,
                session.id,
            ),
        )
        self._conn.commit()
        logger.info(
            f"[DB] 会话结束: id={session.id} | "
            f"关注={session.follows_made} | 原因={session.stop_reason}"
        )

    # ------------------------------------------------------------------ #
    # 统计
    # ------------------------------------------------------------------ #

    def get_followback_rate(self, days: int = 7) -> Optional[float]:
        """统计最近 N 天的回关率（需手动更新 followed_back 字段）。"""
        row = self._conn.execute(
            """
            SELECT
                COUNT(*) AS total,
                SUM(CASE WHEN followed_back = 1 THEN 1 ELSE 0 END) AS backed
            FROM follow_records
            WHERE DATE(followed_at) >= DATE('now', ?)
            """,
            (f"-{days} days",),
        ).fetchone()
        if row and row["total"] > 0:
            return row["backed"] / row["total"]
        return None

    def update_session_account_stats(
        self, session_id: int, my_following: int, my_followers: int
    ) -> None:
        """会话结束后补录账号当前关注数与粉丝数。"""
        self._conn.execute(
            """
            UPDATE run_sessions
            SET my_following_end = ?, my_followers_end = ?
            WHERE id = ?
            """,
            (my_following, my_followers, session_id),
        )
        self._conn.commit()
        logger.info(
            f"[DB] 账号统计已保存: session_id={session_id} "
            f"关注={my_following} 粉丝={my_followers}"
        )

    # ------------------------------------------------------------------ #
    # 已探索博主关注列表 操作
    # ------------------------------------------------------------------ #

    def mark_blogger_following_explored(self, username: str) -> None:
        """标记该博主的关注列表已被完整翻阅过，下次跳过。"""
        self._conn.execute(
            """
            INSERT OR REPLACE INTO explored_following_sources (blogger_username, explored_at)
            VALUES (?, CURRENT_TIMESTAMP)
            """,
            (username,),
        )
        self._conn.commit()
        logger.info(f"[DB] 博主关注列表已标记为已探索: {username}")

    def is_blogger_following_explored(self, username: str) -> bool:
        """该博主的关注列表是否已被完整探索过。"""
        row = self._conn.execute(
            "SELECT 1 FROM explored_following_sources WHERE blogger_username = ?",
            (username,),
        ).fetchone()
        return row is not None

    def close(self) -> None:
        self._conn.close()
