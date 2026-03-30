"""
互关可能性评分算法（满分 100）。

四个维度：
  1. 关注/粉丝比（40分）— 接近 1:1 说明主动互关
  2. 账号规模（20分）    — 中小号（500-5000粉）最可能回关
  3. 内容活跃度（20分）  — 笔记数适中说明活跃
  4. 关注数绝对值（20分）— 关注 300-3000 人说明在建立网络

硬过滤（直接返回 -1，无论得分）：
  - 已关注 / 已互关
  - 蓝 V 认证
  - 粉丝 > 200000
  - 关注数 < 50
  - 笔记数 = 0
"""

from __future__ import annotations

from persistence.models import BloggerProfile


def _follow_ratio_score(followers: int, following: int) -> int:
    """关注/粉丝比得分（0-40）。"""
    if following == 0:
        return 0
    ratio = following / max(followers, 1)
    if 0.7 <= ratio <= 1.5:
        return 40
    if 0.5 <= ratio < 0.7 or 1.5 < ratio <= 2.5:
        return 25
    if 0.3 <= ratio < 0.5 or 2.5 < ratio <= 5.0:
        return 10
    return 0


def _account_size_score(followers: int) -> int:
    """账号规模得分（0-20）。"""
    if 500 <= followers <= 5000:
        return 20
    if 5000 < followers <= 20000:
        return 15
    if 200 <= followers < 500:
        return 10
    if 20000 < followers <= 100000:
        return 5
    return 0  # <200 或 >100000


def _activity_score(notes_count: int) -> int:
    """内容活跃度得分（0-20）。"""
    if 10 <= notes_count <= 200:
        return 20
    if 5 <= notes_count < 10 or 200 < notes_count <= 500:
        return 12
    if 1 <= notes_count < 5:
        return 5
    return 0  # 0 或 >500


def _following_absolute_score(following: int) -> int:
    """关注数绝对值得分（0-20）。"""
    if 300 <= following <= 3000:
        return 20
    if 100 <= following < 300 or 3000 < following <= 5000:
        return 10
    if following > 5000:
        return 0  # 可能是关注机器人
    return 5  # following 很少，保守给分


def compute_follow_score(profile: BloggerProfile) -> int:
    """
    计算互关可能性评分。

    Returns:
        0-100 的整数得分，-1 表示硬过滤直接跳过。
    """
    # 硬过滤
    if profile.is_already_followed or profile.is_mutual_follow:
        return -1
    if profile.is_verified:
        return -1
    if profile.followers > 200000:
        return -1
    if profile.following < 50:
        return -1
    if profile.notes_count == 0:
        return -1

    score = (
        _follow_ratio_score(profile.followers, profile.following)
        + _account_size_score(profile.followers)
        + _activity_score(profile.notes_count)
        + _following_absolute_score(profile.following)
    )
    return min(100, score)
